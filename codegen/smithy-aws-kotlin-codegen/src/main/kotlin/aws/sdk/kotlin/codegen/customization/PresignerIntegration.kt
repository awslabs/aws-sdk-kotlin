package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.core.EndpointResolverGenerator
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.DEFAULT_SOURCE_SET_ROOT
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
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
import software.amazon.smithy.model.shapes.StructureShape
import java.util.logging.Logger

/**
 * This integration applies to any AWS service that provides presign capability on one or more operations.
 *
 * At the time of writing, presigned operations are unmodeled, however it may happen that traits are added
 * to API models in the future to encapsulate this functionality.  As such, this integration is written such
 * that codegen is driven from a du jour model of presign operations.  If and when this state is available
 * in the API model, this informal model should be removed and access to the Smithy model should be used
 * instead to drive codegen.
 */
class PresignerIntegration : KotlinIntegration {

    /**
     * Represents a presignable operation.
     *
     * NOTE: This type intentionally uses serializable types to make future model migration easier.
     */
    data class PresignableOperation(
        /**
         * ID of service presigning applies to
         */
        val serviceId: String,
        /**
         * Operation capable of presigning
         */
        val operationId: String,
        /**
         * (Optional) parameter in which presigned URL should be passed on behalf of the customer
         */
        val presignedParameterId: String?,
        /**
         * HEADER or QUERY_STRING depending on the service and operation.
         */
        val signingLocation: String,
        /**
         * List of header keys that should be included when generating the request signature
         */
        val signedHeaders: Set<String>,
        /**
         * (Optional) override of operation’s HTTP method
         */
        val methodOverride: String?,
        /**
         * true if operation will pass an unsigned body with the request
         */
        val hasBody: Boolean,
        /**
         * If true, map request parameters onto the query string of the presigned URL
         */
        val transformRequestToQueryString: Boolean
    )

    // This is the dejour model that may be replaced by the API model once presign state is available
    private val servicesWithOperationPresigners = listOf(
        PresignableOperation(
            "com.amazonaws.polly#Parrot_v1",
            "com.amazonaws.polly#SynthesizeSpeech",
            null,"QUERY_STRING",
            setOf("host"),
            "GET",
            hasBody = false,
            transformRequestToQueryString = true
        ),
        PresignableOperation(
            "com.amazonaws.s3#AmazonS3",
            "com.amazonaws.s3#GetObject",
            null,
            "HEADER",
            setOf("host", "x-amz-content-sha256", "X-Amz-Date", "Authorization"),
            null,
            hasBody = false,
            transformRequestToQueryString = false
        ),
        PresignableOperation(
            "com.amazonaws.s3#AmazonS3",
            "com.amazonaws.s3#PutObject",
            null,
            "HEADER",
            setOf("host", "x-amz-content-sha256", "X-Amz-Date", "Authorization"),
            null,
            hasBody = true,
            transformRequestToQueryString = false
        )
    )
    private val presignableServiceIds = servicesWithOperationPresigners.map { it.serviceId }.toSet()

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
        AwsRuntimeTypes.Auth.presignUrl,
        AwsRuntimeTypes.Core.Endpoint.EndpointResolver
    )

    private val logger: Logger = Logger.getLogger(PresignerIntegration::class.java.name)

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val currentServiceId = model.expectShape<ServiceShape>(settings.service).id.toString()

        return presignableServiceIds.contains(currentServiceId)
    }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        check(service.hasTrait<SigV4Trait>()) { "Invalid service. Does not specify sigv4 trait: ${service.id}" }

        val presignOperations = servicesWithOperationPresigners.filter {
            it.serviceId == service.id.toString()
        }

        if (presignOperations.isNotEmpty()) {
            renderPresigner(ctx, delegator, service.expectTrait<SigV4Trait>().name, presignOperations)
        } else {
            logger.warning("Service ${service.id} is designated as a service but no operations were specified.")
        }
    }

    private fun renderPresigner(
        ctx: CodegenContext,
        delegator: KotlinDelegator,
        sigv4ServiceName: String,
        presignOperations: List<PresignableOperation>
    ) {
        val writer = KotlinWriter("${ctx.settings.pkg.name}")
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
                definitionFile = "${op.serializerName().capitalize()}.kt"
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
            renderPresignFromClientFn(writer, request.defaultName(serviceShape), requestConfigFnName, serviceSymbol.name)

            // Generate presign config function
            renderPresignConfigFn(writer, request.defaultName(serviceShape), requestConfigFnName, presignableOp, serializerSymbol, request)
        }

        // Generate presign config builder
        val presignConfigTypeName = "${sigv4ServiceName.capitalize()}PresignConfig"
        renderPresignConfigBuilder(writer, presignConfigTypeName, sigv4ServiceName)

        // Write file
        val packagePath = ctx.settings.pkg.name.namespaceToPath()
        delegator.fileManifest.writeFile("$DEFAULT_SOURCE_SET_ROOT$packagePath/Presigner.kt", writer.toString())
    }

    private fun renderPresignConfigBuilder(writer: KotlinWriter, presignConfigTypeName: String, sigv4ServiceName: String) {
        writer.write("""
            /**
             * Provides a subset of the service client configuration necessary to presign a request.
             * This type can be used to presign requests in cases where an existing service client
             * instance is not available.
             */
        """.trimIndent())
        writer.withBlock("class $presignConfigTypeName private constructor(builder: DslBuilder): ServicePresignConfig {", "}\n") {
            write("""
                    override val credentialsProvider: CredentialsProvider = builder.credentialsProvider ?: DefaultChainCredentialsProvider()
                    override val endpointResolver: EndpointResolver = builder.endpointResolver ?: DefaultEndpointResolver()
                    override val region: String = builder.region ?: error("Must specify an AWS region.")
                    override val serviceName: String = "$sigv4ServiceName"
                    
                    companion object {
                        @JvmStatic
                        fun fluentBuilder(): FluentBuilder = BuilderImpl()
                        fun builder(): DslBuilder = BuilderImpl()
                        operator fun invoke(block: DslBuilder.() -> kotlin.Unit): ServicePresignConfig = BuilderImpl().apply(block).build()
                    }
                    
                    interface FluentBuilder {
                        fun credentialsProvider(credentialsProvider: CredentialsProvider): FluentBuilder
                        fun endpointResolver(endpointResolver: EndpointResolver): FluentBuilder
                        fun region(region: String): FluentBuilder
                        fun build(): ServicePresignConfig
                    }
                    
                    interface DslBuilder {
                        /**
                         * The AWS credentials provider to use for authenticating requests. If not provided a
                         * [aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider] instance will be used.
                         */
                        var credentialsProvider: CredentialsProvider?

                        /**
                         * Determines the endpoint (hostname) to make requests to. When not provided a default
                         * resolver is configured automatically. This is an advanced client option.
                         */
                        var endpointResolver: EndpointResolver?

                        /**
                         * AWS region to make requests to
                         */
                        var region: String?

                        fun build(): ServicePresignConfig
                    }
                    
                    internal class BuilderImpl() : FluentBuilder, DslBuilder {
                        override var credentialsProvider: CredentialsProvider? = null
                        override var endpointResolver: EndpointResolver? = null
                        override var region: String? = null
            
                        override fun build(): ServicePresignConfig = $presignConfigTypeName(this)
                        override fun credentialsProvider(credentialsProvider: CredentialsProvider): FluentBuilder =
                            apply { this.credentialsProvider = credentialsProvider }
            
                        override fun endpointResolver(endpointResolver: EndpointResolver): FluentBuilder =
                            apply { this.endpointResolver = endpointResolver }
            
                        override fun region(region: String): FluentBuilder = apply { this.region = region }
                    }
                """.trimIndent())
        }
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
            write("val execContext = ExecutionContext.build {  }")
            write("val httpRequestBuilder = #T().serialize(execContext, request)", serializerSymbol)

            if (!presignableOp.transformRequestToQueryString) {
                write("val path = httpRequestBuilder.url.path")
            } else {
                addImport(RuntimeTypes.Utils.urlEncodeComponent)
                write("val queryStringBuilder = StringBuilder()")
                write("queryStringBuilder.append(httpRequestBuilder.url.path)")
                write("queryStringBuilder.append(\"?\")")
                request.allMembers.forEach { (_, shape) ->
                    withBlock("if (request.${shape.defaultName()} != null) {", "}") {
                        write("queryStringBuilder.append(\"${shape.unionVariantName()}=\${request.${shape.defaultName()}.toString().urlEncodeComponent()}&\")")
                    }
                }
                write("val path = queryStringBuilder.toString()")
            }

            writer.withBlock("return PresignedRequestConfig(", ")") {
                val headerList = presignableOp.signedHeaders.joinToString(separator = ",", ) { it.doubleQuote() }
                write("setOf($headerList),")
                if (presignableOp.methodOverride != null) {
                    addImport(RuntimeTypes.Http.HttpMethod)
                    write("#T.${presignableOp.methodOverride},", RuntimeTypes.Http.HttpMethod)
                } else {
                    write("httpRequestBuilder.method,")
                }
                write("path,")
                write("durationSeconds.toLong(),")
                write("${presignableOp.hasBody},")
                write("SigningLocation.${presignableOp.signingLocation}")
            }
        }
    }

    private fun renderPresignFromClientFn(
        writer: KotlinWriter,
        requestTypeName: String,
        requestConfigFnName: String,
        serviceClientTypeName: String
    ) {
        writer.write("""
                /**
                 * Presign a [$requestTypeName] using a [$serviceClientTypeName].
                 * @param serviceClient the client providing properties used to generate the presigned request. 
                 * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
                 * @return The [PresignedRequest] that can be invoked within the specified time window.
                 */
            """.trimIndent())
        // FIXME ~ Replace or add additional function, swap ULong type for kotlin.time.Duration when type becomes stable
        writer.withBlock("suspend fun $requestTypeName.presign(serviceClient: $serviceClientTypeName, durationSeconds: ULong): PresignedRequest {", "}\n") {
            write("""
                    val serviceClientConfig = object : ServicePresignConfig {
                        override val region: String = requireNotNull(serviceClient.config.region) { "Service client must set a region." }
                        override val serviceName: String = serviceClient.serviceName
                        override val endpointResolver: EndpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()
                        override val credentialsProvider: CredentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()
                    }
                """.trimIndent())
            write("return presignUrl(serviceClientConfig, $requestConfigFnName(this, durationSeconds))")
        }
    }

    private fun renderPresignFromConfigFn(writer: KotlinWriter, requestTypeName: String, requestConfigFnName: String) {
        writer.write("""
                /**
                 * Presign a [$requestTypeName] using a [ServicePresignConfig].
                 * @param serviceClientConfig the client configuration used to generate the presigned request
                 * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity. 
                 * @return The [PresignedRequest] that can be invoked within the specified time window.
                 */
            """.trimIndent(), )
        // FIXME ~ Replace or add additional function, swap ULong type for kotlin.time.Duration when type becomes stable
        writer.withBlock("suspend fun $requestTypeName.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): PresignedRequest {", "}\n") {
            write("return presignUrl(serviceClientConfig, $requestConfigFnName(this, durationSeconds))")
        }
    }
}