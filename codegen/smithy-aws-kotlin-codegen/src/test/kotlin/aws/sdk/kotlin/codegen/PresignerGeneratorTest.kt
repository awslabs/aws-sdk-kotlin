package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.Presignable
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import kotlin.test.Test
import kotlin.test.assertTrue

class PresignerGeneratorTest {
    private val testModel = """
            namespace smithy.kotlin.traits

            use aws.protocols#awsJson1_0
            use aws.auth#sigv4
            use aws.api#service

            @trait(selector: "*")
            structure presignable { }
            
            @awsJson1_0
            @sigv4(name: "example-signing-name")
            @service(sdkId: "example")
            service Example {
                version: "1.0.0",
                operations: [GetFoo, PostFoo, PutFoo]
            }

            @presignable
            @http(method: "POST", uri: "/foo")
            operation PostFoo {
                input: GetFooInput
            }
            
            @presignable
            @readonly
            @http(method: "GET", uri: "/foo")
            operation GetFoo { }
            
            @presignable
            @http(method: "PUT", uri: "/foo")
            @idempotent
            operation PutFoo {
                input: GetFooInput
            }

            structure GetFooInput {
                payload: String
            }            
        """.toSmithyModel()
    private val testContext = testModel.newTestContext("Example", "smithy.kotlin.traits")

    private val codegenContext = object : CodegenContext {
        override val model: Model = testContext.generationCtx.model
        override val symbolProvider: SymbolProvider = testContext.generationCtx.symbolProvider
        override val settings: KotlinSettings = testContext.generationCtx.settings
        override val protocolGenerator: ProtocolGenerator? = testContext.generator
        override val integrations: List<KotlinIntegration> = testContext.generationCtx.integrations
    }

    @Test
    fun testCustomTraitOnModel() {
        assertTrue(testModel.expectShape<OperationShape>("smithy.kotlin.traits#GetFoo").hasTrait(Presignable.ID))
    }

    // TODO ~ exercise both awsQuery and restXml protocols
    @Test
    fun testPresignerCodegenNoBody() {
        val unit = PresignerGenerator()

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        testContext.generationCtx.delegator.flushWriters()
        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("src/main/kotlin/smithy/kotlin/traits/Presigner.kt")

        val expected = """
            package smithy.kotlin.traits
            
            import aws.sdk.kotlin.runtime.ClientException
            import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
            import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
            import aws.sdk.kotlin.runtime.auth.signing.PresignedRequestConfig
            import aws.sdk.kotlin.runtime.auth.signing.ServicePresignConfig
            import aws.sdk.kotlin.runtime.auth.signing.SigningLocation
            import aws.sdk.kotlin.runtime.auth.signing.createPresignedRequest
            import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
            import aws.smithy.kotlin.runtime.client.ExecutionContext
            import aws.smithy.kotlin.runtime.http.QueryParameters
            import aws.smithy.kotlin.runtime.http.request.HttpRequest
            import smithy.kotlin.traits.internal.DefaultEndpointResolver
            import smithy.kotlin.traits.model.GetFooRequest
            import smithy.kotlin.traits.model.PostFooRequest
            import smithy.kotlin.traits.model.PutFooRequest
            import smithy.kotlin.traits.transform.GetFooOperationSerializer
            import smithy.kotlin.traits.transform.PostFooOperationSerializer
            import smithy.kotlin.traits.transform.PutFooOperationSerializer
            
            /**
             * Presign a [GetFooRequest] using a [ServicePresignConfig].
             * @param presignConfig the configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            suspend fun GetFooRequest.presign(presignConfig: ServicePresignConfig, durationSeconds: Long): HttpRequest {
                return createPresignedRequest(presignConfig, getFooPresignConfig(this, durationSeconds))
            }
            
            /**
             * Presign a [GetFooRequest] using a [TestClient].
             * @param config the client configuration used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            suspend fun GetFooRequest.presign(config: TestClient.Config, durationSeconds: Long): HttpRequest {
                val presignConfig = TestPresignConfig {
                    credentialsProvider = config.credentialsProvider
                    endpointResolver = config.endpointResolver
                    region = config.region
                }
                return createPresignedRequest(presignConfig, getFooPresignConfig(this, durationSeconds))
            }
            
            private suspend fun getFooPresignConfig(input: GetFooRequest, durationSeconds: Long) : PresignedRequestConfig {
                require(durationSeconds > 0) { "duration must be greater than zero" }
                val httpRequestBuilder = GetFooOperationSerializer().serialize(ExecutionContext.build {  }, input)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    durationSeconds.toLong(),
                    false,
                    SigningLocation.HEADER
                )
            }
            
            /**
             * Presign a [PostFooRequest] using a [ServicePresignConfig].
             * @param presignConfig the configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            suspend fun PostFooRequest.presign(presignConfig: ServicePresignConfig, durationSeconds: Long): HttpRequest {
                return createPresignedRequest(presignConfig, postFooPresignConfig(this, durationSeconds))
            }
            
            /**
             * Presign a [PostFooRequest] using a [TestClient].
             * @param config the client configuration used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            suspend fun PostFooRequest.presign(config: TestClient.Config, durationSeconds: Long): HttpRequest {
                val presignConfig = TestPresignConfig {
                    credentialsProvider = config.credentialsProvider
                    endpointResolver = config.endpointResolver
                    region = config.region
                }
                return createPresignedRequest(presignConfig, postFooPresignConfig(this, durationSeconds))
            }
            
            private suspend fun postFooPresignConfig(input: PostFooRequest, durationSeconds: Long) : PresignedRequestConfig {
                require(durationSeconds > 0) { "duration must be greater than zero" }
                val httpRequestBuilder = PostFooOperationSerializer().serialize(ExecutionContext.build {  }, input)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    durationSeconds.toLong(),
                    false,
                    SigningLocation.HEADER
                )
            }
            
            /**
             * Presign a [PutFooRequest] using a [ServicePresignConfig].
             * @param presignConfig the configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            suspend fun PutFooRequest.presign(presignConfig: ServicePresignConfig, durationSeconds: Long): HttpRequest {
                return createPresignedRequest(presignConfig, putFooPresignConfig(this, durationSeconds))
            }
            
            /**
             * Presign a [PutFooRequest] using a [TestClient].
             * @param config the client configuration used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            suspend fun PutFooRequest.presign(config: TestClient.Config, durationSeconds: Long): HttpRequest {
                val presignConfig = TestPresignConfig {
                    credentialsProvider = config.credentialsProvider
                    endpointResolver = config.endpointResolver
                    region = config.region
                }
                return createPresignedRequest(presignConfig, putFooPresignConfig(this, durationSeconds))
            }
            
            private suspend fun putFooPresignConfig(input: PutFooRequest, durationSeconds: Long) : PresignedRequestConfig {
                require(durationSeconds > 0) { "duration must be greater than zero" }
                val httpRequestBuilder = PutFooOperationSerializer().serialize(ExecutionContext.build {  }, input)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    durationSeconds.toLong(),
                    false,
                    SigningLocation.HEADER
                )
            }
            
            /**
             * Provides a subset of the service client configuration necessary to presign a request.
             * This type can be used to presign requests in cases where an existing service client
             * instance is not available.
             */
            class TestPresignConfig private constructor(builder: Builder): ServicePresignConfig {
                override val credentialsProvider: CredentialsProvider = builder.credentialsProvider ?: DefaultChainCredentialsProvider()
                override val endpointResolver: AwsEndpointResolver = builder.endpointResolver ?: DefaultEndpointResolver()
                override val region: String = requireNotNull(builder.region) { "region is a required configuration property" }
                override val serviceId: String = "example"
                override val signingName: String = "example-signing-name"
                companion object {
                    inline operator fun invoke(block: Builder.() -> kotlin.Unit): ServicePresignConfig = Builder().apply(block).build()
                }
            
                class Builder {
                    /**
                     * The AWS credentials provider to use for authenticating requests. If not provided a [aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider] instance will be used.
                     */
                    var credentialsProvider: CredentialsProvider? = null
                    /**
                     * Determines the endpoint (hostname) to make requests to. When not provided a default resolver is configured automatically. This is an advanced client option.
                     */
                    var endpointResolver: AwsEndpointResolver? = null
                    /**
                     * AWS region to make requests for
                     */
                    var region: String? = null
            
                    @PublishedApi
                    internal fun build(): TestPresignConfig = TestPresignConfig(this)
                }
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }
}
