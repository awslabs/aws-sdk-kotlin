package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.Presignable
import org.junit.jupiter.api.Test
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
            import aws.sdk.kotlin.runtime.auth.CredentialsProvider
            import aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider
            import aws.sdk.kotlin.runtime.auth.PresignedRequest
            import aws.sdk.kotlin.runtime.auth.PresignedRequestConfig
            import aws.sdk.kotlin.runtime.auth.ServicePresignConfig
            import aws.sdk.kotlin.runtime.auth.SigningLocation
            import aws.sdk.kotlin.runtime.auth.createPresignedRequest
            import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
            import aws.smithy.kotlin.runtime.client.ExecutionContext
            import aws.smithy.kotlin.runtime.http.QueryParameters
            import smithy.kotlin.traits.internal.DefaultEndpointResolver
            import smithy.kotlin.traits.model.GetFooRequest
            import smithy.kotlin.traits.model.PostFooRequest
            import smithy.kotlin.traits.model.PutFooRequest
            import smithy.kotlin.traits.transform.GetFooOperationSerializer
            import smithy.kotlin.traits.transform.PostFooOperationSerializer
            import smithy.kotlin.traits.transform.PutFooOperationSerializer
            
            /**
             * Presign a [GetFooRequest] using a [ServicePresignConfig].
             * @param serviceClientConfig the client configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun GetFooRequest.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): PresignedRequest {
                return createPresignedRequest(serviceClientConfig, getFooPresignConfig(this, durationSeconds))
            }
            
            /**
             * Presign a [GetFooRequest] using a [TestClient].
             * @param serviceClient the client providing properties used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun GetFooRequest.presign(serviceClient: TestClient, durationSeconds: ULong): PresignedRequest {
                val serviceClientConfig = TestPresignConfig {
                    credentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()
                    endpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()
                    region = requireNotNull(serviceClient.config.region) { "Service client must set a region." }
                }
                return createPresignedRequest(serviceClientConfig, getFooPresignConfig(this, durationSeconds))
            }
            
            private suspend fun getFooPresignConfig(request: GetFooRequest, durationSeconds: ULong) : PresignedRequestConfig {
                require(durationSeconds > 0u) { "duration must be greater than zero" }
                val httpRequestBuilder = GetFooOperationSerializer().serialize(ExecutionContext.build {  }, request)
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
             * @param serviceClientConfig the client configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun PostFooRequest.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): PresignedRequest {
                return createPresignedRequest(serviceClientConfig, postFooPresignConfig(this, durationSeconds))
            }
            
            /**
             * Presign a [PostFooRequest] using a [TestClient].
             * @param serviceClient the client providing properties used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun PostFooRequest.presign(serviceClient: TestClient, durationSeconds: ULong): PresignedRequest {
                val serviceClientConfig = TestPresignConfig {
                    credentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()
                    endpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()
                    region = requireNotNull(serviceClient.config.region) { "Service client must set a region." }
                }
                return createPresignedRequest(serviceClientConfig, postFooPresignConfig(this, durationSeconds))
            }
            
            private suspend fun postFooPresignConfig(request: PostFooRequest, durationSeconds: ULong) : PresignedRequestConfig {
                require(durationSeconds > 0u) { "duration must be greater than zero" }
                val httpRequestBuilder = PostFooOperationSerializer().serialize(ExecutionContext.build {  }, request)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    durationSeconds.toLong(),
                    true,
                    SigningLocation.HEADER
                )
            }
            
            /**
             * Presign a [PutFooRequest] using a [ServicePresignConfig].
             * @param serviceClientConfig the client configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun PutFooRequest.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): PresignedRequest {
                return createPresignedRequest(serviceClientConfig, putFooPresignConfig(this, durationSeconds))
            }
            
            /**
             * Presign a [PutFooRequest] using a [TestClient].
             * @param serviceClient the client providing properties used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun PutFooRequest.presign(serviceClient: TestClient, durationSeconds: ULong): PresignedRequest {
                val serviceClientConfig = TestPresignConfig {
                    credentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()
                    endpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()
                    region = requireNotNull(serviceClient.config.region) { "Service client must set a region." }
                }
                return createPresignedRequest(serviceClientConfig, putFooPresignConfig(this, durationSeconds))
            }
            
            private suspend fun putFooPresignConfig(request: PutFooRequest, durationSeconds: ULong) : PresignedRequestConfig {
                require(durationSeconds > 0u) { "duration must be greater than zero" }
                val httpRequestBuilder = PutFooOperationSerializer().serialize(ExecutionContext.build {  }, request)
                return PresignedRequestConfig(
                    httpRequestBuilder.method,
                    httpRequestBuilder.url.path,
                    httpRequestBuilder.url.parameters.build(),
                    durationSeconds.toLong(),
                    true,
                    SigningLocation.HEADER
                )
            }
            
            /**
             * Provides a subset of the service client configuration necessary to presign a request.
             * This type can be used to presign requests in cases where an existing service client
             * instance is not available.
             */
            class TestPresignConfig private constructor(builder: BuilderImpl): ServicePresignConfig {
                override val credentialsProvider: CredentialsProvider = builder.credentialsProvider
                override val endpointResolver: EndpointResolver = builder.endpointResolver
                override val region: String = builder.region ?: throw ClientException("region must be set")
                override val serviceId: String = "example"
                override val signingName: String = "example-signing-name"
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
                    fun build(): TestPresignConfig
                }
            
                interface DslBuilder {
                    /**
                     * The AWS credentials provider to use for authenticating requests. If not provided a [aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider] instance will be used.
                     */
                    var credentialsProvider: CredentialsProvider
            
                    /**
                     * Determines the endpoint (hostname) to make requests to. When not provided a default resolver is configured automatically. This is an advanced client option.
                     */
                    var endpointResolver: EndpointResolver
            
                    /**
                     * AWS region to make requests for
                     */
                    var region: String?
            
                    fun build(): TestPresignConfig
                }
            
                internal class BuilderImpl() : FluentBuilder, DslBuilder {
                    override var credentialsProvider: CredentialsProvider = DefaultChainCredentialsProvider()
                    override var endpointResolver: EndpointResolver = DefaultEndpointResolver()
                    override var region: String? = null
            
                    override fun build(): TestPresignConfig = TestPresignConfig(this)
                    override fun credentialsProvider(credentialsProvider: CredentialsProvider): FluentBuilder = apply { this.credentialsProvider = credentialsProvider }
                    override fun endpointResolver(endpointResolver: EndpointResolver): FluentBuilder = apply { this.endpointResolver = endpointResolver }
                    override fun region(region: String): FluentBuilder = apply { this.region = region }
                }
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }
}
