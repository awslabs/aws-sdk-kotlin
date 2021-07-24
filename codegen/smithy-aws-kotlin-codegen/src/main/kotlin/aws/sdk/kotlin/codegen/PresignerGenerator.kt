package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.customization.PresignTraitIntegration
import aws.sdk.kotlin.codegen.protocols.core.EndpointResolverGenerator
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.DEFAULT_SOURCE_SET_ROOT
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigGenerator
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.serde.serializerName
import software.amazon.smithy.kotlin.codegen.utils.namespaceToPath
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.HttpTrait
import java.util.logging.Logger

/**
 * Represents a presignable operation.
 *
 * @property serviceId ID of service presigning applies to
 * @property operationId Operation capable of presigning
 * @property presignedParameterId (Optional) parameter in which presigned URL should be passed in the request
 * @property hasBody true if operation will pass an unsigned body with the request
 *
 */
data class PresignableOperation(
    val serviceId: String,
    val operationId: String,
    // TODO ~ Implementation of embedded presigned URLs is TBD
    val presignedParameterId: String?,
    val hasBody: Boolean,
)

/**
 * This integration applies to any AWS service that provides presign capability on one or more operations.
 */
class PresignerGenerator : KotlinIntegration {
    // Symbols which should be imported
    private val presignerRuntimeSymbols = setOf(
        // smithy-kotlin types
        RuntimeTypes.Core.ExecutionContext,
        // AWS types
        AwsRuntimeTypes.Auth.CredentialsProvider,
        AwsRuntimeTypes.Auth.DefaultChainCredentialsProvider,
        AwsRuntimeTypes.Auth.PresignedRequest,
        AwsRuntimeTypes.Auth.PresignedRequestConfig,
        AwsRuntimeTypes.Auth.ServicePresignConfig,
        AwsRuntimeTypes.Auth.SigningLocation,
        AwsRuntimeTypes.Auth.createPresignedRequest,
        AwsRuntimeTypes.Core.Endpoint.EndpointResolver
    )

    private val logger: Logger = Logger.getLogger(PresignerGenerator::class.java.name)

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)

        // Only services with SigV4 are currently presignable
        if (!service.hasTrait<SigV4Trait>()) return

        // Scan model for operations decorated with PresignTrait
        val presignOperations = service.allOperations
            .map { ctx.model.expectShape(it) }
            .filter { operationShape -> operationShape.hasTrait(PresignTraitIntegration.PresignTrait.shapeId) }
            .map { operationShape ->
                val hasBody = operationShape.expectTrait<HttpTrait>().method != "GET"
                PresignableOperation(service.id.toString(), operationShape.id.toString(), null, hasBody)
            }

        // If presignable operations found for this service, generate a Presigner file
        if (presignOperations.isNotEmpty()) {
            renderPresigner(ctx, delegator, service.expectTrait<SigV4Trait>().name, presignOperations)
        }
    }

    private fun renderPresigner(
        ctx: CodegenContext,
        delegator: KotlinDelegator,
        sigv4ServiceName: String,
        presignOperations: List<PresignableOperation>
    ) {
        val writer = KotlinWriter(ctx.settings.pkg.name)
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)
        val defaultEndpointResolverSymbol = buildSymbol {
            namespace = "${ctx.settings.pkg.name}.internal"
            name = EndpointResolverGenerator.typeName
        }
        // import RT types
        writer.addImport(presignerRuntimeSymbols)

        // import generated SDK types
        writer.addImport(serviceSymbol)
        writer.addImport(defaultEndpointResolverSymbol)

        // Iterate over each presignable operation and generate presign functions
        presignOperations.forEach { presignableOp ->
            val op = ctx.model.expectShape<OperationShape>(presignableOp.operationId)
            val request = ctx.model.expectShape<StructureShape>(op.input.get())
            val serializerSymbol = buildSymbol {
                definitionFile = "${op.serializerName()}.kt"
                name = op.serializerName()
                namespace = "${ctx.settings.pkg.name}.transform"
            }

            // import operation symbols
            writer.addImport(ctx.symbolProvider.toSymbol(request))
            writer.addImport(serializerSymbol)

            val requestConfigFnName = "${op.defaultName()}PresignConfig"

            // Generate config presign function
            renderPresignFromConfigFn(writer, request.defaultName(serviceShape), requestConfigFnName)

            // Generate input presign extension function for service client
            renderPresignFromClientFn(writer, request.defaultName(serviceShape), requestConfigFnName, serviceSymbol.name, sigv4ServiceName)

            // Generate presign config function
            renderPresignConfigFn(writer, request.defaultName(serviceShape), requestConfigFnName, presignableOp, serializerSymbol, request)
        }

        // Generate presign config builder
        val presignConfigTypeName = "${ctx.settings.sdkId}PresignConfig"
        val rc = RenderingContext(writer, serviceShape, ctx.model, ctx.symbolProvider, ctx.settings)
        renderPresignConfigBuilder(writer, presignConfigTypeName, sigv4ServiceName, rc)

        // Write file
        val packagePath = ctx.settings.pkg.name.namespaceToPath()
        delegator.fileManifest.writeFile("$DEFAULT_SOURCE_SET_ROOT$packagePath/Presigner.kt", writer.toString())
    }

    private fun renderPresignConfigBuilder(writer: KotlinWriter, presignConfigTypeName: String, sigv4ServiceName: String, renderingContext: RenderingContext<ServiceShape>) {
        writer.dokka {
            write("Provides a subset of the service client configuration necessary to presign a request.")
            write("This type can be used to presign requests in cases where an existing service client")
            write("instance is not available.")
        }
        writer.addImport(AwsRuntimeTypes.Core.ClientException)
        writer.putContext("configClass.name", presignConfigTypeName)
        val credentialsProviderProperty = ClientConfigProperty {
            symbol = AwsRuntimeTypes.Auth.CredentialsProvider
            name = "credentialsProvider"
            documentation = "The AWS credentials provider to use for authenticating requests. If not provided a [aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider] instance will be used."
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            defaultValue = "DefaultChainCredentialsProvider()"
        }
        val endpointResolverProperty = ClientConfigProperty {
            symbol = AwsRuntimeTypes.Core.Endpoint.EndpointResolver
            name = "endpointResolver"
            documentation = "Determines the endpoint (hostname) to make requests to. When not provided a default resolver is configured automatically. This is an advanced client option."
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            defaultValue = "DefaultEndpointResolver()"
        }
        val region = ClientConfigProperty {
            symbol = buildSymbol {
                name = "String"
                namespace = "kotlin"
                nullable = true
            }
            name = "region"
            documentation = "AWS region to make requests to"
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            defaultValue = """throw ClientException("Must specify a region")"""
        }
        val serviceNameProperty = ClientConfigProperty {
            symbol = KotlinTypes.String
            name = "serviceName"
            documentation = "docs"
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            constantValue = """"$sigv4ServiceName""""
        }
        val ccg = ClientConfigGenerator(renderingContext, false, AwsRuntimeTypes.Auth.ServicePresignConfig, credentialsProviderProperty, endpointResolverProperty, region, serviceNameProperty)
        ccg.render()
    }

    private fun renderPresignConfigFn(
        writer: KotlinWriter,
        requestTypeName: String,
        requestConfigFnName: String,
        presignableOp: PresignableOperation,
        serializerSymbol: Symbol,
        request: StructureShape
    ) {
        writer.withBlock("private suspend fun $requestConfigFnName(request: $requestTypeName, durationSeconds: ULong) : PresignedRequestConfig {", "}\n") {
            write("require(durationSeconds > 0u) { \"duration must be greater than zero\" }")
            write("val httpRequestBuilder = #T().serialize(ExecutionContext.build {  }, request)", serializerSymbol)

            writer.withBlock("return PresignedRequestConfig(", ")") {
                write("httpRequestBuilder.method,")
                write("httpRequestBuilder.url.path,")
                addImport(RuntimeTypes.Http.QueryParameters)
                write("httpRequestBuilder.url.parameters.build(),")
                write("durationSeconds.toLong(),")
                write("${presignableOp.hasBody},")
                write("SigningLocation.HEADER")
            }
        }
    }

    private fun renderPresignFromClientFn(
        writer: KotlinWriter,
        requestTypeName: String,
        requestConfigFnName: String,
        serviceClientTypeName: String,
        sigv4ServiceName: String,
    ) {
        writer.dokka {
            write("Presign a [$requestTypeName] using a [$serviceClientTypeName].")
            write("@param serviceClient the client providing properties used to generate the presigned request.")
            write("@param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.")
            write("@return The [PresignedRequest] that can be invoked within the specified time window.")
        }
        // FIXME ~ Replace or add additional function, swap ULong type for kotlin.time.Duration when type becomes stable
        writer.withBlock("suspend fun $requestTypeName.presign(serviceClient: $serviceClientTypeName, durationSeconds: ULong): PresignedRequest {", "}\n") {
            write(
                """
                    val serviceClientConfig = object : ServicePresignConfig {
                        override val region: String = requireNotNull(serviceClient.config.region) { "Service client must set a region." }
                        override val serviceName: String = "$sigv4ServiceName"
                        override val endpointResolver: EndpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()
                        override val credentialsProvider: CredentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()
                    }
                """.trimIndent()
            )
            write("return createPresignedRequest(serviceClientConfig, $requestConfigFnName(this, durationSeconds))")
        }
    }

    private fun renderPresignFromConfigFn(writer: KotlinWriter, requestTypeName: String, requestConfigFnName: String) {
        writer.dokka {
            write("Presign a [$requestTypeName] using a [ServicePresignConfig].")
            write("@param serviceClientConfig the client configuration used to generate the presigned request")
            write("@param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.")
            write("@return The [PresignedRequest] that can be invoked within the specified time window.")
        }
        // FIXME ~ Replace or add additional function, swap ULong type for kotlin.time.Duration when type becomes stable
        writer.withBlock("suspend fun $requestTypeName.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): PresignedRequest {", "}\n") {
            write("return createPresignedRequest(serviceClientConfig, $requestConfigFnName(this, durationSeconds))")
        }
    }
}
