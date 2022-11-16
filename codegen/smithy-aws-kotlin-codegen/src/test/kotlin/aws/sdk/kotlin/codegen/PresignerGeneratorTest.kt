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
            use smithy.rules#endpointRuleSet

            @trait(selector: "*")
            structure presignable { }
            
            @awsJson1_0
            @sigv4(name: "example-signing-name")
            @service(sdkId: "example")
            @endpointRuleSet(
                version: "1.0",
                parameters: {},
                rules: [
                    {
                        "type": "endpoint",
                        "conditions": [],
                        "endpoint": {
                            "url": "https://static.endpoint.test"
                        }
                    }
                ]
            )
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
        val actual = testManifest.expectFileString("src/main/kotlin/smithy/kotlin/traits/presigners/Presigners.kt")

        val expected = """
            package smithy.kotlin.traits.presigners
            
            import aws.sdk.kotlin.runtime.ClientException
            import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
            import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
            import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
            import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
            import aws.smithy.kotlin.runtime.auth.awssigning.PresignedRequestConfig
            import aws.smithy.kotlin.runtime.auth.awssigning.PresigningLocation
            import aws.smithy.kotlin.runtime.auth.awssigning.ServicePresignConfig
            import aws.smithy.kotlin.runtime.auth.awssigning.SigningEndpointProvider
            import aws.smithy.kotlin.runtime.auth.awssigning.createPresignedRequest
            import aws.smithy.kotlin.runtime.client.ExecutionContext
            import aws.smithy.kotlin.runtime.http.QueryParameters
            import aws.smithy.kotlin.runtime.http.request.HttpRequest
            import kotlin.time.Duration
            import smithy.kotlin.traits.TestClient
            import smithy.kotlin.traits.endpoints.EndpointParameters
            import smithy.kotlin.traits.endpoints.asSigningProvider
            import smithy.kotlin.traits.endpoints.internal.bindAwsBuiltins
            import smithy.kotlin.traits.model.GetFooRequest
            import smithy.kotlin.traits.model.PostFooRequest
            import smithy.kotlin.traits.model.PutFooRequest
            import smithy.kotlin.traits.transform.GetFooOperationSerializer
            import smithy.kotlin.traits.transform.PostFooOperationSerializer
            import smithy.kotlin.traits.transform.PutFooOperationSerializer
            
            /**
             * Presign a [GetFooRequest] using a [ServicePresignConfig].
             * @param presignConfig the configuration used to generate the presigned request
             * @param duration the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            public suspend fun GetFooRequest.presign(presignConfig: ServicePresignConfig, duration: Duration): HttpRequest {
                return createPresignedRequest(presignConfig, getFooPresignConfig(this, duration))
            }
            
            /**
             * Presign a [GetFooRequest] using a [TestClient].
             * @param config the client configuration used to generate the presigned request.
             * @param duration the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            public suspend fun GetFooRequest.presign(config: TestClient.Config, duration: Duration): HttpRequest {
                val presignConfig = TestPresignConfig {
                    val params = EndpointParameters {
                        bindAwsBuiltins(config)
                    }
                    credentialsProvider = config.credentialsProvider
                    endpointProvider = config.endpointProvider.asSigningProvider(params)
                    region = config.region
                }
                return createPresignedRequest(presignConfig, getFooPresignConfig(this, duration))
            }
            
            private suspend fun getFooPresignConfig(input: GetFooRequest, duration: Duration) : PresignedRequestConfig {
                require(duration.isPositive()) { "duration must be greater than zero" }
                val httpRequestBuilder = GetFooOperationSerializer().serialize(ExecutionContext.build {  }, input)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    duration,
                    false,
                    PresigningLocation.QUERY_STRING,
                    httpRequestBuilder.headers.build(),
                )
            }
            
            /**
             * Presign a [PostFooRequest] using a [ServicePresignConfig].
             * @param presignConfig the configuration used to generate the presigned request
             * @param duration the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            public suspend fun PostFooRequest.presign(presignConfig: ServicePresignConfig, duration: Duration): HttpRequest {
                return createPresignedRequest(presignConfig, postFooPresignConfig(this, duration))
            }
            
            /**
             * Presign a [PostFooRequest] using a [TestClient].
             * @param config the client configuration used to generate the presigned request.
             * @param duration the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            public suspend fun PostFooRequest.presign(config: TestClient.Config, duration: Duration): HttpRequest {
                val presignConfig = TestPresignConfig {
                    val params = EndpointParameters {
                        bindAwsBuiltins(config)
                    }
                    credentialsProvider = config.credentialsProvider
                    endpointProvider = config.endpointProvider.asSigningProvider(params)
                    region = config.region
                }
                return createPresignedRequest(presignConfig, postFooPresignConfig(this, duration))
            }
            
            private suspend fun postFooPresignConfig(input: PostFooRequest, duration: Duration) : PresignedRequestConfig {
                require(duration.isPositive()) { "duration must be greater than zero" }
                val httpRequestBuilder = PostFooOperationSerializer().serialize(ExecutionContext.build {  }, input)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    duration,
                    false,
                    PresigningLocation.QUERY_STRING,
                    httpRequestBuilder.headers.build(),
                )
            }
            
            /**
             * Presign a [PutFooRequest] using a [ServicePresignConfig].
             * @param presignConfig the configuration used to generate the presigned request
             * @param duration the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            public suspend fun PutFooRequest.presign(presignConfig: ServicePresignConfig, duration: Duration): HttpRequest {
                return createPresignedRequest(presignConfig, putFooPresignConfig(this, duration))
            }
            
            /**
             * Presign a [PutFooRequest] using a [TestClient].
             * @param config the client configuration used to generate the presigned request.
             * @param duration the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [HttpRequest] that can be invoked within the specified time window.
             */
            public suspend fun PutFooRequest.presign(config: TestClient.Config, duration: Duration): HttpRequest {
                val presignConfig = TestPresignConfig {
                    val params = EndpointParameters {
                        bindAwsBuiltins(config)
                    }
                    credentialsProvider = config.credentialsProvider
                    endpointProvider = config.endpointProvider.asSigningProvider(params)
                    region = config.region
                }
                return createPresignedRequest(presignConfig, putFooPresignConfig(this, duration))
            }
            
            private suspend fun putFooPresignConfig(input: PutFooRequest, duration: Duration) : PresignedRequestConfig {
                require(duration.isPositive()) { "duration must be greater than zero" }
                val httpRequestBuilder = PutFooOperationSerializer().serialize(ExecutionContext.build {  }, input)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    duration,
                    false,
                    PresigningLocation.QUERY_STRING,
                    httpRequestBuilder.headers.build(),
                )
            }
            
            /**
             * Provides a subset of the service client configuration necessary to presign a request.
             * This type can be used to presign requests in cases where an existing service client
             * instance is not available.
             */
            public class TestPresignConfig private constructor(builder: Builder): ServicePresignConfig {
                override val credentialsProvider: CredentialsProvider = requireNotNull(builder.credentialsProvider) { "credentialsProvider is a required configuration property" }
                override val endpointProvider: SigningEndpointProvider = requireNotNull(builder.endpointProvider) { "endpointProvider is a required configuration property" }
                override val normalizeUriPath: Boolean = true
                override val region: String = requireNotNull(builder.region) { "region is a required configuration property" }
                override val serviceId: String = "example"
                override val signer: AwsSigner = builder.signer ?: DefaultAwsSigner
                override val signingName: String = "example-signing-name"
                override val useDoubleUriEncode: Boolean = true
                public companion object {
                    public inline operator fun invoke(block: Builder.() -> kotlin.Unit): ServicePresignConfig = Builder().apply(block).build()
                }
            
                public class Builder {
                    /**
                     * The AWS credentials provider to use for authenticating requests. If not provided a [aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider] instance will be used.
                     */
                    public var credentialsProvider: CredentialsProvider? = null
                    /**
                     * Provides the endpoint (hostname) and signing context to make requests to.
                     */
                    public var endpointProvider: SigningEndpointProvider? = null
                    /**
                     * AWS region to make requests for
                     */
                    public var region: String? = null
                    /**
                     * The implementation of AWS signer to use for signing requests
                     */
                    public var signer: AwsSigner? = null
            
                    @PublishedApi
                    internal fun build(): TestPresignConfig = TestPresignConfig(this)
                }
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }
}
