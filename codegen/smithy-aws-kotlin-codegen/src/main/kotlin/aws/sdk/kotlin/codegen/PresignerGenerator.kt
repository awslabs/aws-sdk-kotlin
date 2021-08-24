package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.Presignable
import aws.sdk.kotlin.codegen.protocols.core.EndpointResolverGenerator
import aws.sdk.kotlin.codegen.protocols.core.QueryBindingResolver
import aws.sdk.kotlin.codegen.protocols.middleware.AwsSignatureVersion4
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.core.clientName
import software.amazon.smithy.kotlin.codegen.core.declareSection
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionId
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigGenerator
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.hasHttpBody
import software.amazon.smithy.kotlin.codegen.rendering.serde.serializerName
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.TimestampFormatTrait

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

    /**
     * Identifies the [PresignConfigFn] section for overriding generated implementation.
     */
    object PresignConfigFnSection : SectionId {
        const val CodegenContext = "CodegenContext"
        const val OperationId = "OperationId"
        const val HttpBindingResolver = "HttpBindingResolver"
        const val DefaultTimestampFormat = "DefaultTimestampFormat"
    }

    // Symbols which should be imported
    private val presignerRuntimeSymbols = setOf(
        // smithy-kotlin types
        RuntimeTypes.Http.Request.HttpRequest,
        RuntimeTypes.Core.ExecutionContext,
        // AWS types
        AwsRuntimeTypes.Auth.CredentialsProvider,
        AwsRuntimeTypes.Auth.DefaultChainCredentialsProvider,
        AwsRuntimeTypes.Auth.PresignedRequestConfig,
        AwsRuntimeTypes.Auth.ServicePresignConfig,
        AwsRuntimeTypes.Auth.SigningLocation,
        AwsRuntimeTypes.Auth.createPresignedRequest,
        AwsRuntimeTypes.Core.Endpoint.EndpointResolver
    )

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val protocolGenerator: HttpBindingProtocolGenerator = ctx.protocolGenerator as HttpBindingProtocolGenerator
        val httpBindingResolver = protocolGenerator.getProtocolHttpBindingResolver(ctx.model, service)
        val defaultTimestampFormat = protocolGenerator.defaultTimestampFormat

        // Only services with SigV4 are currently presignable
        if (!AwsSignatureVersion4.isSupportedAuthentication(ctx.model, service)) return

        // Scan model for operations decorated with PresignTrait
        val presignOperations = service.allOperations
            .map { ctx.model.expectShape<OperationShape>(it) }
            .filter { operationShape -> operationShape.hasTrait(Presignable.ID) }
            .map { operationShape ->
                check(AwsSignatureVersion4.hasSigV4AuthScheme(ctx.model, service, operationShape)) { "Operation does not have valid auth trait" }
                val resolver: HttpBindingResolver = getProtocolHttpBindingResolver(ctx, service)
                val hasBody = resolver.hasHttpBody(operationShape)
                PresignableOperation(service.id.toString(), operationShape.id.toString(), null, hasBody)
            }

        // If presignable operations found for this service, generate a Presigner file
        if (presignOperations.isNotEmpty()) {
            delegator.useFileWriter("Presigner.kt", ctx.settings.pkg.name) { writer ->
                renderPresigner(writer, ctx, httpBindingResolver, service.expectTrait<SigV4Trait>().name, presignOperations, defaultTimestampFormat)
            }
        }
    }

    private fun renderPresigner(
        writer: KotlinWriter,
        ctx: CodegenContext,
        httpBindingResolver: HttpBindingResolver,
        sigv4ServiceName: String,
        presignOperations: List<PresignableOperation>,
        defaultTimestampFormat: TimestampFormatTrait.Format
    ) {
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)
        val defaultEndpointResolverSymbol = buildSymbol {
            namespace = "${ctx.settings.pkg.name}.internal"
            name = EndpointResolverGenerator.typeName
        }
        val clientName = clientName(ctx.settings.sdkId)
        val presignConfigTypeName = "${clientName}PresignConfig"

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
            renderPresignFromClientFn(writer, request.defaultName(serviceShape), requestConfigFnName, serviceSymbol.name, presignConfigTypeName)

            // Generate presign config function
            val contextMap = mapOf(
                PresignConfigFnSection.OperationId to presignableOp.operationId,
                PresignConfigFnSection.CodegenContext to ctx,
                PresignConfigFnSection.HttpBindingResolver to httpBindingResolver,
                PresignConfigFnSection.DefaultTimestampFormat to defaultTimestampFormat
            )

            renderPresignConfigFn(
                writer, request.defaultName(serviceShape), requestConfigFnName, presignableOp,
                serializerSymbol, presignConfigFnVisitorFactory(ctx.protocolGenerator!!.protocol),
                contextMap
            )
        }

        // Generate presign config builder
        val rc = RenderingContext(writer, serviceShape, ctx.model, ctx.symbolProvider, ctx.settings)
        renderPresignConfigBuilder(writer, presignConfigTypeName, sigv4ServiceName, serviceShape.sdkId, rc)
    }

    private fun renderPresignConfigBuilder(writer: KotlinWriter, presignConfigTypeName: String, sigv4ServiceName: String, serviceId: String, renderingContext: RenderingContext<ServiceShape>) {
        writer.dokka {
            write("Provides a subset of the service client configuration necessary to presign a request.")
            write("This type can be used to presign requests in cases where an existing service client")
            write("instance is not available.")
        }
        writer.addImport(AwsRuntimeTypes.Core.ClientException)
        writer.putContext("configClass.name", presignConfigTypeName)
        val credentialsProviderProperty = ClientConfigProperty {
            symbol = buildSymbol {
                name = "CredentialsProvider"
                defaultValue = "DefaultChainCredentialsProvider()"
                namespace(AwsKotlinDependency.AWS_AUTH)
                nullable = false
            }
            name = "credentialsProvider"
            documentation = "The AWS credentials provider to use for authenticating requests. If not provided a [aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider] instance will be used."
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
        }
        val endpointResolverProperty = ClientConfigProperty {
            symbol = buildSymbol {
                name = "EndpointResolver"
                namespace(AwsKotlinDependency.AWS_CORE, "endpoint")
                defaultValue = "DefaultEndpointResolver()"
                nullable = false
            }
            name = "endpointResolver"
            documentation = "Determines the endpoint (hostname) to make requests to. When not provided a default resolver is configured automatically. This is an advanced client option."
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
        }
        val region = ClientConfigProperty {
            symbol = buildSymbol {
                name = "String"
                namespace = "kotlin"
                nullable = true
            }
            name = "region"
            documentation = "AWS region to make requests for"
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            required = true
        }
        val signingNameProperty = ClientConfigProperty {
            symbol = KotlinTypes.String
            name = "signingName"
            documentation = "Service identifier used to sign requests"
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            constantValue = """"$sigv4ServiceName""""
        }
        val serviceIdProperty = ClientConfigProperty {
            symbol = KotlinTypes.String
            name = "serviceId"
            documentation = "Service identifier used to resolve endpoints"
            baseClass = AwsRuntimeTypes.Auth.ServicePresignConfig
            constantValue = """"$serviceId""""
        }

        val ccg = ClientConfigGenerator(
            renderingContext,
            false,
            AwsRuntimeTypes.Auth.ServicePresignConfig,
            credentialsProviderProperty,
            endpointResolverProperty,
            region,
            signingNameProperty,
            serviceIdProperty
        )
        ccg.render()
    }

    interface PresignConfigFnVisitor {
        fun renderHttpMethod(writer: KotlinWriter)
        fun renderQueryParameters(writer: KotlinWriter)
    }

    private fun presignConfigFnVisitorFactory(protocol: ShapeId): PresignConfigFnVisitor =
        when (protocol) {
            RestJson1Trait.ID,
            RestXmlTrait.ID -> {
                object : PresignConfigFnVisitor {
                    override fun renderHttpMethod(writer: KotlinWriter) {
                        writer.write("httpRequestBuilder.method,")
                    }

                    override fun renderQueryParameters(writer: KotlinWriter) {
                        writer.addImport(RuntimeTypes.Http.QueryParameters)
                        writer.write("httpRequestBuilder.url.parameters.build(),")
                    }
                }
            }
            AwsQueryTrait.ID -> {
                object : PresignConfigFnVisitor {
                    override fun renderHttpMethod(writer: KotlinWriter) {
                        writer.addImport(RuntimeTypes.Http.HttpMethod)
                        writer.write("HttpMethod.GET,")
                    }

                    override fun renderQueryParameters(writer: KotlinWriter) {
                        writer.addImport(RuntimeTypes.Http.QueryParameters)
                        writer.addImport(RuntimeTypes.Http.toByteStream)
                        writer.addImport(RuntimeTypes.Core.Content.decodeToString)
                        writer.addImport(RuntimeTypes.Http.splitAsQueryParameters)
                        writer.write("""httpRequestBuilder.body.toByteStream()?.decodeToString()?.splitAsQueryParameters() ?: QueryParameters.Empty,""")
                    }
                }
            }
            else -> throw CodegenException("Unhandled protocol $protocol")
        }

    private fun renderPresignConfigFn(
        writer: KotlinWriter,
        requestTypeName: String,
        requestConfigFnName: String,
        presignableOp: PresignableOperation,
        serializerSymbol: Symbol,
        presignConfigFnVisitor: PresignConfigFnVisitor,
        contextMap: Map<String, Any?>
    ) {
        writer.withBlock("private suspend fun $requestConfigFnName(input: $requestTypeName, durationSeconds: ULong) : PresignedRequestConfig {", "}\n") {

            writer.declareSection(PresignConfigFnSection, contextMap) {
                write("require(durationSeconds > 0u) { \"duration must be greater than zero\" }")
                write("val httpRequestBuilder = #T().serialize(ExecutionContext.build {  }, input)", serializerSymbol)

                writer.withBlock("return PresignedRequestConfig(", ")") {
                    presignConfigFnVisitor.renderHttpMethod(this)
                    write("httpRequestBuilder.url.path,")
                    presignConfigFnVisitor.renderQueryParameters(writer)
                    write("durationSeconds.toLong(),")
                    write("${presignableOp.hasBody},")
                    write("SigningLocation.HEADER")
                }
            }
        }
    }

    private fun renderPresignFromClientFn(
        writer: KotlinWriter,
        requestTypeName: String,
        requestConfigFnName: String,
        serviceClientTypeName: String,
        presignConfigTypeName: String,
    ) {
        writer.dokka {
            write("Presign a [$requestTypeName] using a [$serviceClientTypeName].")
            write("@param serviceClient the client providing properties used to generate the presigned request.")
            write("@param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.")
            write("@return The [HttpRequest] that can be invoked within the specified time window.")
        }
        // FIXME ~ Replace or add additional function, swap ULong type for kotlin.time.Duration when type becomes stable
        writer.withBlock("suspend fun $requestTypeName.presign(serviceClient: $serviceClientTypeName, durationSeconds: ULong): HttpRequest {", "}\n") {
            withBlock("val serviceClientConfig = $presignConfigTypeName {", "}") {
                write("credentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()")
                write("endpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()")
                write("region = requireNotNull(serviceClient.config.region) { \"Service client must set a region.\" }")
            }
            write("return createPresignedRequest(serviceClientConfig, $requestConfigFnName(this, durationSeconds))")
        }
    }

    private fun renderPresignFromConfigFn(writer: KotlinWriter, requestTypeName: String, requestConfigFnName: String) {
        writer.dokka {
            write("Presign a [$requestTypeName] using a [ServicePresignConfig].")
            write("@param serviceClientConfig the client configuration used to generate the presigned request")
            write("@param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.")
            write("@return The [HttpRequest] that can be invoked within the specified time window.")
        }
        // FIXME ~ Replace or add additional function, swap ULong type for kotlin.time.Duration when type becomes stable
        writer.withBlock("suspend fun $requestTypeName.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): HttpRequest {", "}\n") {
            write("return createPresignedRequest(serviceClientConfig, $requestConfigFnName(this, durationSeconds))")
        }
    }

    private fun getProtocolHttpBindingResolver(ctx: CodegenContext, service: ServiceShape): HttpBindingResolver =
        when (requireNotNull(ctx.protocolGenerator).protocol) {
            AwsQueryTrait.ID -> QueryBindingResolver(ctx.model, service)
            RestJson1Trait.ID -> HttpTraitResolver(ctx.model, service, "application/json")
            RestXmlTrait.ID -> HttpTraitResolver(ctx.model, service, "application/xml")
            else -> throw CodegenException("Unable to create HttpBindingResolver for unhandled protocol ${ctx.protocolGenerator?.protocol}")
        }
}
