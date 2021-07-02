package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.sdkId
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.codegen.core.SymbolReference
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.DEFAULT_SOURCE_SET_ROOT
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.unionVariantName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.serde.serializerName
import software.amazon.smithy.kotlin.codegen.utils.doubleQuote
import software.amazon.smithy.kotlin.codegen.utils.namespaceToPath
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.HttpTrait

data class PresignableOperation(
    val serviceName: String,
    val operation: ShapeId,
    val signingLocation: String,
    val signedHeaders: Set<String>,
    val methodOverride: String? = null,
    val hasBody: Boolean = false,
    val transformRequestToQueryString: Boolean = false
)

class GeneralPresignerIntegration : KotlinIntegration {

    // FIXME ~ this model data may eventually be added to Smithy.  If so this entire integration
    //  should be removed and the logic should move to the AWS SDK.
    private val servicesWithOperationPresigners = listOf(
        PresignableOperation("polly", ShapeId.from("com.amazonaws.polly#SynthesizeSpeech"), "QUERY_STRING", setOf("host"), "GET", hasBody = false, transformRequestToQueryString = true),
        PresignableOperation("s3", ShapeId.from("com.amazonaws.s3#GetObject"), "HEADER", setOf("host", "x-amz-content-sha256", "X-Amz-Date", "Authorization")),
        PresignableOperation("s3", ShapeId.from("com.amazonaws.s3#PutObject"), "HEADER", setOf("host", "x-amz-content-sha256", "X-Amz-Date", "Authorization"), null, true)
    )

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val currentSdk = model.expectShape<ServiceShape>(settings.service).sdkId.toLowerCase()

        val serviceNames = servicesWithOperationPresigners.map { it.serviceName }.toSet()

        return serviceNames.contains(currentSdk)
    }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        check(service.hasTrait<SigV4Trait>()) { "Invalid service. Does not specify sigv4 trait: ${service.id}" }

        val presignOperations = servicesWithOperationPresigners.filter { it.serviceName.equals(service.expectTrait<ServiceTrait>().sdkId, ignoreCase = true) }

        if (presignOperations.isNotEmpty()) {
            generatePresignerFile(ctx, delegator, service.expectTrait<SigV4Trait>().name, presignOperations)
        }
    }

    private fun generatePresignerFile(
        ctx: CodegenContext,
        delegator: KotlinDelegator,
        sigv4ServiceName: String,
        presignOperations: List<PresignableOperation>
    ) {
        val writer = KotlinWriter("${ctx.settings.pkg.name}.internal")
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)

        writer.addImport(RuntimeTypes.Http.HttpMethod)
        writer.addImport(RuntimeTypes.Core.ExecutionContext)
        writer.addImport(RuntimeTypes.Utils.urlEncodeComponent)

        writer.addImport(AwsRuntimeTypes.Auth.CredentialsProvider)
        writer.addImport(AwsRuntimeTypes.Auth.DefaultChainCredentialsProvider)
        writer.addImport(AwsRuntimeTypes.Auth.PresignedRequest)
        writer.addImport(AwsRuntimeTypes.Auth.PresignedRequestConfig)
        writer.addImport(AwsRuntimeTypes.Auth.ServicePresignConfig)
        writer.addImport(AwsRuntimeTypes.Auth.SigningLocation)
        writer.addImport(AwsRuntimeTypes.Auth.presignUrl)
        writer.addImport(AwsRuntimeTypes.Core.Endpoint.EndpointResolver)

        presignOperations.forEach { presignableOp ->
            val op = ctx.model.expectShape<OperationShape>(presignableOp.operation)
            val request = ctx.model.expectShape<StructureShape>(op.input.get())

            writer.addImport(ctx.symbolProvider.toSymbol(request))

            val requestConfigFnName = "${op.defaultName()}PresignConfig"

            // Generate presign function
            writer.withBlock("suspend fun ${request.defaultName(serviceShape)}.presign(serviceClientConfig: ServicePresignConfig): PresignedRequest {", "}") {
                writer.write("return presignUrl(serviceClientConfig, $requestConfigFnName(this))")
            }

            val serializerSymbol = buildSymbol {
                definitionFile = "${op.serializerName().capitalize()}.kt"
                name = op.serializerName()
                namespace = "${ctx.settings.pkg.name}.transform"
            }
            writer.addImport(serializerSymbol)

            // Generate presign config function
            writer.withBlock("private suspend fun $requestConfigFnName(request: ${request.defaultName(serviceShape)}) : PresignedRequestConfig {", "}") {
                writer.write("val execContext = ExecutionContext.build {  }")
                writer.write("val httpRequestBuilder = #T().serialize(execContext, request)", serializerSymbol)

                if (!presignableOp.transformRequestToQueryString) {
                    writer.write("val path = httpRequestBuilder.url.path")
                } else {
                    writer.write("val queryStringBuilder = StringBuilder()")
                    writer.write("queryStringBuilder.append(httpRequestBuilder.url.path)")
                    writer.write("queryStringBuilder.append(\"?\")")
                    request.allMembers.forEach { (_, shape) ->
                        writer.withBlock("if (request.${shape.defaultName()} != null) {", "}") {
                            writer.write("queryStringBuilder.append(\"${shape.unionVariantName()}=\${request.${shape.defaultName()}.toString().urlEncodeComponent()}&\")")
                        }
                    }
                    writer.write("val path = queryStringBuilder.toString()")
                }

                writer.withBlock("return PresignedRequestConfig(", ")") {
                    val headerList = presignableOp.signedHeaders.joinToString(separator = ",", ) { it.doubleQuote() }
                    writer.write("setOf($headerList),")
                    if (presignableOp.methodOverride != null) {
                        writer.write("HttpMethod.${presignableOp.methodOverride},")
                    } else {
                        writer.write("httpRequestBuilder.method,")
                    }
                    writer.write("path,")
                    writer.write("60,")
                    writer.write("${presignableOp.hasBody},")
                    writer.write("SigningLocation.${presignableOp.signingLocation}")
                }
            }
        }

        val packagePath = ctx.settings.pkg.name.namespaceToPath()
        delegator.fileManifest.writeFile("$DEFAULT_SOURCE_SET_ROOT$packagePath/internal/Presigner.kt", writer.toString())
    }
}