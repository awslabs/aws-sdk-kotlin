/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.Presignable
import aws.sdk.kotlin.codegen.protocols.endpoints.getAsSigningProviderExtSymbol
import aws.sdk.kotlin.codegen.protocols.endpoints.renderAsSigningProviderExt
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
import software.amazon.smithy.kotlin.codegen.integration.SectionKey
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointResolverAdapterGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.serde.serializerName
import software.amazon.smithy.kotlin.codegen.rendering.util.AbstractConfigGenerator
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigPropertyType
import software.amazon.smithy.kotlin.codegen.utils.dq
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
 * @property signBody true if the body is to be read and signed, otherwise body specified as unsigned.
 *
 */
data class PresignableOperation(
    val serviceId: String,
    val operationId: String,
    val signBody: Boolean,
)

/**
 * This integration applies to any AWS service that provides presign capability on one or more operations.
 */
class PresignerGenerator : KotlinIntegration {
    /**
     * Identifies the [PresignConfigFnSection] section for overriding generated implementation.
     */
    object PresignConfigFnSection : SectionId {
        val CodegenContext: SectionKey<CodegenContext> = SectionKey("CodegenContext")
        val OperationId: SectionKey<String> = SectionKey("OperationId")
        val HttpBindingResolver: SectionKey<HttpBindingResolver> = SectionKey("HttpBindingResolver")
        val DefaultTimestampFormat: SectionKey<TimestampFormatTrait.Format> = SectionKey("DefaultTimestampFormat")
    }

    // Symbols which should be imported
    private val presignerRuntimeSymbols = setOf(
        // smithy-kotlin types
        RuntimeTypes.Http.Request.HttpRequest,
        RuntimeTypes.Core.ExecutionContext,
        // AWS types
        RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider,
        AwsRuntimeTypes.Config.Credentials.DefaultChainCredentialsProvider,
        RuntimeTypes.Auth.Signing.AwsSigningCommon.PresignedRequestConfig,
        RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig,
        RuntimeTypes.Auth.Signing.AwsSigningCommon.createPresignedRequest,
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
                check(
                    AwsSignatureVersion4.hasSigV4AuthScheme(
                        ctx.model,
                        service,
                        operationShape,
                    ),
                ) { "Operation does not have valid auth trait" }
                val protocol = requireNotNull(ctx.protocolGenerator).protocol.name
                val shouldSignBody = signBody(protocol)

                PresignableOperation(service.id.toString(), operationShape.id.toString(), shouldSignBody)
            }

        // If presignable operations found for this service, generate a Presigner file
        if (presignOperations.isNotEmpty()) {
            delegator.useFileWriter("Presigners.kt", "${ctx.settings.pkg.name}.presigners") { writer ->
                renderPresigner(
                    writer,
                    ctx,
                    httpBindingResolver,
                    service.expectTrait<SigV4Trait>().name,
                    presignOperations,
                    defaultTimestampFormat,
                )

                // render presign extension that re-uses the endpoint resolver adapter for presigning
                writer.write("")
                EndpointResolverAdapterGenerator.renderAsSigningProviderExt(ctx.settings, writer)
            }
        }
    }

    // Determine if body should be read and signed by CRT.  If body is to be signed by CRT, null is passed to signer
    // for signedBodyValue parameter. This causes CRT to read the body and compute the signature.
    // Otherwise, AwsSignedBodyValue.UNSIGNED_PAYLOAD is passed specifying that the body will be ignored and CRT
    // will not take the body into account when signing the request.
    private fun signBody(protocol: String) =
        when (protocol) {
            "awsQuery" -> true // Query protocol always contains a body
            else -> false
        }

    private fun renderPresigner(
        writer: KotlinWriter,
        ctx: CodegenContext,
        httpBindingResolver: HttpBindingResolver,
        sigv4ServiceName: String,
        presignOperations: List<PresignableOperation>,
        defaultTimestampFormat: TimestampFormatTrait.Format,
    ) {
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)
        val clientName = clientName(ctx.settings.sdkId)
        val presignConfigTypeName = "${clientName}PresignConfig"

        // import RT types
        writer.addImport(presignerRuntimeSymbols)

        // import generated SDK types
        writer.addImport(serviceSymbol)

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
            renderPresignFromClientFn(
                ctx,
                writer,
                request.defaultName(serviceShape),
                requestConfigFnName,
                serviceSymbol.name,
                presignConfigTypeName,
                op,
            )

            // Generate presign config function
            val contextMap: Map<SectionKey<*>, Any> = mapOf(
                PresignConfigFnSection.OperationId to presignableOp.operationId,
                PresignConfigFnSection.CodegenContext to ctx,
                PresignConfigFnSection.HttpBindingResolver to httpBindingResolver,
                PresignConfigFnSection.DefaultTimestampFormat to defaultTimestampFormat,
            )

            renderPresignConfigFn(
                writer,
                request.defaultName(serviceShape),
                requestConfigFnName,
                presignableOp,
                serializerSymbol,
                presignConfigFnVisitorFactory(ctx.protocolGenerator!!.protocol),
                contextMap,
            )
        }

        // Generate presign config builder
        val clientProperties: List<ConfigProperty> = getClientProperties(sigv4ServiceName, serviceShape.sdkId)
        val rc = RenderingContext(writer, serviceShape, ctx.model, ctx.symbolProvider, ctx.settings)
        renderPresignConfigBuilder(writer, presignConfigTypeName, rc, clientProperties)
    }

    private fun renderPresignConfigBuilder(
        writer: KotlinWriter,
        presignConfigTypeName: String,
        renderingContext: RenderingContext<ServiceShape>,
        clientProperties: List<ConfigProperty>,
    ) {
        writer.dokka {
            write("Provides a subset of the service client configuration necessary to presign a request.")
            write("This type can be used to presign requests in cases where an existing service client")
            write("instance is not available.")
        }
        writer.addImport(AwsRuntimeTypes.Core.ClientException)
        writer.putContext("configClass.name", presignConfigTypeName)

        val ccg = object : AbstractConfigGenerator() {}
        ccg.render(renderingContext, clientProperties, renderingContext.writer)
    }

    // Captures protocol-specific state needed for presigning requests
    interface PresignConfigFnVisitor {
        fun renderHttpMethod(writer: KotlinWriter)
        fun renderQueryParameters(writer: KotlinWriter)
    }

    private fun presignConfigFnVisitorFactory(protocol: ShapeId): PresignConfigFnVisitor =
        when (protocol) {
            RestJson1Trait.ID,
            RestXmlTrait.ID,
            -> {
                object : PresignConfigFnVisitor {
                    override fun renderHttpMethod(writer: KotlinWriter) {
                        writer.write("httpRequestBuilder.method,")
                    }

                    override fun renderQueryParameters(writer: KotlinWriter) {
                        writer.addImport(RuntimeTypes.Core.Net.QueryParameters)
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
                        writer.addImport(RuntimeTypes.Core.Net.QueryParameters)
                        writer.addImport(RuntimeTypes.Http.toByteStream)
                        writer.addImport(RuntimeTypes.Core.Content.decodeToString)
                        writer.addImport(RuntimeTypes.Core.Net.splitAsQueryParameters)
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
        contextMap: Map<SectionKey<*>, Any?>,
    ) {
        writer.withBlock(
            "private suspend fun $requestConfigFnName(input: $requestTypeName, duration: #T) : #T {",
            "}\n",
            KotlinTypes.Time.Duration,
            RuntimeTypes.Auth.Signing.AwsSigningCommon.PresignedRequestConfig,
        ) {
            writer.declareSection(PresignConfigFnSection, contextMap) {
                write("require(duration.isPositive()) { \"duration must be greater than zero\" }")
                write("val httpRequestBuilder = #T().serialize(ExecutionContext.build {  }, input)", serializerSymbol)

                writer.withBlock("return #T(", ")", RuntimeTypes.Auth.Signing.AwsSigningCommon.PresignedRequestConfig) {
                    presignConfigFnVisitor.renderHttpMethod(this)
                    write("httpRequestBuilder.url.path,")
                    presignConfigFnVisitor.renderQueryParameters(writer)
                    write("duration,")
                    write("${presignableOp.signBody},")
                    write("#T.QUERY_STRING,", RuntimeTypes.Auth.Signing.AwsSigningCommon.PresigningLocation)
                    write("httpRequestBuilder.headers.build(),")
                }
            }
        }
    }

    private fun renderPresignFromClientFn(
        ctx: CodegenContext,
        writer: KotlinWriter,
        requestTypeName: String,
        requestConfigFnName: String,
        serviceClientTypeName: String,
        presignConfigTypeName: String,
        op: OperationShape,
    ) {
        writer.dokka {
            write("Presign a [$requestTypeName] using a [$serviceClientTypeName].")
            write("@param config the client configuration used to generate the presigned request.")
            write("@param duration the amount of time from signing for which the request is valid, with seconds granularity.")
            write("@return The [HttpRequest] that can be invoked within the specified time window.")
        }

        writer
            .addImport(KotlinTypes.Time.Duration)
            .withBlock(
                "public suspend fun #L.presign(config: #L.Config, duration: #T): HttpRequest {",
                "}\n",
                requestTypeName,
                serviceClientTypeName,
                KotlinTypes.Time.Duration,
            ) {
                withBlock("val presignConfig = $presignConfigTypeName {", "}") {
                    write("credentialsProvider = config.credentialsProvider")
                    write(
                        "endpointProvider = #T(config).#T(this@presign, #S)",
                        EndpointResolverAdapterGenerator.getSymbol(ctx.settings),
                        EndpointResolverAdapterGenerator.getAsSigningProviderExtSymbol(ctx.settings),
                        op.id.name,
                    )
                    write("region = config.region")
                }
                write("return createPresignedRequest(presignConfig, $requestConfigFnName(this, duration))")
            }
    }

    private fun renderPresignFromConfigFn(writer: KotlinWriter, requestTypeName: String, requestConfigFnName: String) {
        writer.dokka {
            write("Presign a [$requestTypeName] using a [ServicePresignConfig].")
            write("@param presignConfig the configuration used to generate the presigned request")
            write("@param duration the amount of time from signing for which the request is valid, with seconds granularity.")
            write("@return The [HttpRequest] that can be invoked within the specified time window.")
        }
        // FIXME ~ Replace or add additional function, swap Long type for kotlin.time.Duration when type becomes stable
        writer
            .addImport(KotlinTypes.Time.Duration)
            .withBlock(
                "public suspend fun #L.presign(presignConfig: ServicePresignConfig, duration: #T): HttpRequest {",
                "}\n",
                requestTypeName,
                KotlinTypes.Time.Duration,
            ) {
                write("return createPresignedRequest(presignConfig, $requestConfigFnName(this, duration))")
            }
    }

    // Provide all client properties for a presigner client
    private fun getClientProperties(sigv4ServiceName: String, serviceId: String): List<ConfigProperty> =
        listOf(
            ConfigProperty {
                symbol = RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigner
                name = "signer"
                documentation = "The implementation of AWS signer to use for signing requests"
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.RequiredWithDefault("DefaultAwsSigner")
                additionalImports = listOf(
                    RuntimeTypes.Auth.Signing.AwsSigningStandard.DefaultAwsSigner,
                )
            },
            ConfigProperty {
                symbol = RuntimeTypes.Auth.Credentials.AwsCredentials.CredentialsProvider
                name = "credentialsProvider"
                documentation =
                    "The AWS credentials provider to use for authenticating requests. If not provided a [aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider] instance will be used."
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.Required()
            },
            ConfigProperty {
                symbol = RuntimeTypes.Auth.Signing.AwsSigningCommon.SigningEndpointProvider
                name = "endpointProvider"
                documentation =
                    "Provides the endpoint (hostname) and signing context to make requests to."
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.Required()
            },
            ConfigProperty {
                symbol = buildSymbol {
                    name = "String"
                    namespace = "kotlin"
                    nullable = true
                }
                name = "region"
                documentation = "AWS region to make requests for"
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.Required()
            },
            ConfigProperty {
                symbol = KotlinTypes.String
                name = "signingName"
                documentation = "Service identifier used to sign requests"
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.ConstantValue(sigv4ServiceName.dq())
            },
            ConfigProperty {
                symbol = KotlinTypes.String
                name = "serviceId"
                documentation = "Service identifier used to resolve endpoints"
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.ConstantValue(serviceId.dq())
            },
            ConfigProperty {
                symbol = buildSymbol {
                    name = "Boolean"
                    namespace = "kotlin"
                    nullable = true
                }
                name = "useDoubleUriEncode"
                documentation = "Determines if presigner should double encode Uri"
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.ConstantValue(useDoubleUriEncodeValueForService(serviceId))
            },
            ConfigProperty {
                symbol = buildSymbol {
                    name = "Boolean"
                    namespace = "kotlin"
                    nullable = true
                }
                name = "normalizeUriPath"
                documentation = "Determines if presigned URI path will be normalized"
                baseClass = RuntimeTypes.Auth.Signing.AwsSigningCommon.ServicePresignConfig
                propertyType = ConfigPropertyType.ConstantValue(normalizeUriPathValueForService(serviceId))
            },
        )

    // Determine useDoubleUriEncode setting based on service
    // From https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html:
    //   "Each path segment must be URI-encoded twice (except for Amazon S3 which only gets URI-encoded once)."
    private fun useDoubleUriEncodeValueForService(serviceId: String): String =
        when (serviceId) {
            "S3" -> false
            else -> true
        }.toString()

    // Determine normalizeUriPath setting based on service
    // From https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html#canonical-request:
    //   "You do not normalize URI paths for requests to Amazon S3. For example, you may have a bucket with an object named "my-object//example//photo.user". Normalizing the path changes the object name in the request to "my-object/example/photo.user". This is an incorrect path for that object."
    private fun normalizeUriPathValueForService(serviceId: String): String =
        useDoubleUriEncodeValueForService(serviceId)
}
