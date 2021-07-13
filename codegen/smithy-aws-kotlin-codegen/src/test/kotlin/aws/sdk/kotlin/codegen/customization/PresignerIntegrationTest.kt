package aws.sdk.kotlin.codegen.customization

import org.junit.jupiter.api.Test
import software.amazon.smithy.build.MockManifest
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.test.formatForTest
import software.amazon.smithy.kotlin.codegen.test.newTestContext
import software.amazon.smithy.kotlin.codegen.test.shouldContainOnlyOnceWithDiff
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresignerIntegrationTest {

    val testModel = """
            namespace smithy.example

            use aws.protocols#awsJson1_0
            use aws.auth#sigv4

            @awsJson1_0
            @sigv4(name: "example-signing-name")
            service Example {
                version: "1.0.0",
                operations: [GetFoo]
            }

            operation GetFoo {
                input: GetFooInput
            }
            
            structure GetFooInput {
                payload: String
            }
        """.toSmithyModel()

    private val testContext = testModel.newTestContext("Example", "smithy.example")

    private val codegenContext = object : CodegenContext {
        override val model: Model = testContext.generationCtx.model
        override val symbolProvider: SymbolProvider = testContext.generationCtx.symbolProvider
        override val settings: KotlinSettings = testContext.generationCtx.settings
        override val protocolGenerator: ProtocolGenerator? = null
        override val integrations: List<KotlinIntegration> = testContext.generationCtx.integrations
    }

    @Test
    fun testServiceEnablementInclusion() {
        // NOTE ~ when/if presign state is added to api models this test model should be removed
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf(),
                null,
                false,
                false
            )
        )

        val unit = PresignerIntegration(testPresignerModel)

        assertTrue(unit.enabledForService(testModel, testContext.generationCtx.settings))
    }

    @Test
    fun testServiceEnablementExclusion() {
        // NOTE ~ when/if presign state is added to api models this test model should be removed
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#NoPresignableOpsHere",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf(),
                null,
                false,
                false
            )
        )

        val unit = PresignerIntegration(testPresignerModel)

        assertFalse(unit.enabledForService(testModel, testContext.generationCtx.settings))
    }

    @Test
    fun testPresignOperationCodegen() {
        // NOTE ~ when/if presign state is added to api models this test model should be removed
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf(),
                null,
                false,
                false
            )
        )
        val unit = PresignerIntegration(testPresignerModel)

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("/./src/main/kotlin/smithy/example/Presigner.kt")

        val expected = """
            package smithy.example

            import aws.sdk.kotlin.runtime.auth.CredentialsProvider
            import aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider
            import aws.sdk.kotlin.runtime.auth.PresignedRequest
            import aws.sdk.kotlin.runtime.auth.PresignedRequestConfig
            import aws.sdk.kotlin.runtime.auth.ServicePresignConfig
            import aws.sdk.kotlin.runtime.auth.SigningLocation
            import aws.sdk.kotlin.runtime.auth.presignUrl
            import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
            import aws.smithy.kotlin.runtime.client.ExecutionContext
            import smithy.example.internal.DefaultEndpointResolver
            import smithy.example.model.GetFooRequest
            import smithy.example.transform.GetFooOperationSerializer

            /**
             * Presign a [GetFooRequest] using a [ServicePresignConfig].
             * @param serviceClientConfig the client configuration used to generate the presigned request
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun GetFooRequest.presign(serviceClientConfig: ServicePresignConfig, durationSeconds: ULong): PresignedRequest {
                return presignUrl(serviceClientConfig, getFooPresignConfig(this, durationSeconds))
            }

            /**
             * Presign a [GetFooRequest] using a [TestClient].
             * @param serviceClient the client providing properties used to generate the presigned request.
             * @param durationSeconds the amount of time from signing for which the request is valid, with seconds granularity.
             * @return The [PresignedRequest] that can be invoked within the specified time window.
             */
            suspend fun GetFooRequest.presign(serviceClient: TestClient, durationSeconds: ULong): PresignedRequest {
                val serviceClientConfig = object : ServicePresignConfig {
                    override val region: String = requireNotNull(serviceClient.config.region) { "Service client must set a region." }
                    override val serviceName: String = serviceClient.serviceName
                    override val endpointResolver: EndpointResolver = serviceClient.config.endpointResolver ?: DefaultEndpointResolver()
                    override val credentialsProvider: CredentialsProvider = serviceClient.config.credentialsProvider ?: DefaultChainCredentialsProvider()
                }
                return presignUrl(serviceClientConfig, getFooPresignConfig(this, durationSeconds))
            }

            private suspend fun getFooPresignConfig(request: GetFooRequest, durationSeconds: ULong) : PresignedRequestConfig {
                require(durationSeconds > 0u) { "duration must be greater than zero" }
                val execContext = ExecutionContext.build {  }
                val httpRequestBuilder = GetFooOperationSerializer().serialize(execContext, request)
                val path = httpRequestBuilder.url.path
                return PresignedRequestConfig(
                    setOf(),
                    httpRequestBuilder.method,
                    path,
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
            class Example-signing-namePresignConfig private constructor(builder: DslBuilder): ServicePresignConfig {
                override val credentialsProvider: CredentialsProvider = builder.credentialsProvider ?: DefaultChainCredentialsProvider()
                override val endpointResolver: EndpointResolver = builder.endpointResolver ?: DefaultEndpointResolver()
                override val region: String = builder.region ?: error("Must specify an AWS region.")
                override val serviceName: String = "example-signing-name"

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

                    override fun build(): ServicePresignConfig = Example-signing-namePresignConfig(this)
                    override fun credentialsProvider(credentialsProvider: CredentialsProvider): FluentBuilder =
                        apply { this.credentialsProvider = credentialsProvider }

                    override fun endpointResolver(endpointResolver: EndpointResolver): FluentBuilder =
                        apply { this.endpointResolver = endpointResolver }

                    override fun region(region: String): FluentBuilder = apply { this.region = region }
                }
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignOperationCodegenQueryString() {
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "QUERY_STRING",
                setOf(),
                null,
                false,
                false
            )
        )
        val unit = PresignerIntegration(testPresignerModel)

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("/./src/main/kotlin/smithy/example/Presigner.kt")

        val expected = """
            return PresignedRequestConfig(
                setOf(),
                httpRequestBuilder.method,
                path,
                durationSeconds.toLong(),
                false,
                SigningLocation.QUERY_STRING
            )
        """.formatForTest("    ")

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignOperationCodegenWithCustomHeaders() {
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf("Header-A", "Header-B"),
                null,
                false,
                false
            )
        )
        val unit = PresignerIntegration(testPresignerModel)

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("/./src/main/kotlin/smithy/example/Presigner.kt")

        val expected = """
            return PresignedRequestConfig(
                setOf("Header-A","Header-B"),
                httpRequestBuilder.method,
                path,
                durationSeconds.toLong(),
                false,
                SigningLocation.HEADER
            )
        """.formatForTest("    ")

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignOperationCodegenOveriddenMethod() {
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf(),
                "PUT",
                false,
                false
            )
        )
        val unit = PresignerIntegration(testPresignerModel)

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("/./src/main/kotlin/smithy/example/Presigner.kt")

        val expected = """
                return PresignedRequestConfig(
                    setOf(),
                    HttpMethod.PUT,
                    path,
                    durationSeconds.toLong(),
                    false,
                    SigningLocation.HEADER
                )
            """.formatForTest("    ")

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignOperationCodegenWithBody() {
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf(),
                null,
                true,
                false
            )
        )
        val unit = PresignerIntegration(testPresignerModel)

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("/./src/main/kotlin/smithy/example/Presigner.kt")

        val expected = """
            return PresignedRequestConfig(
                setOf(),
                httpRequestBuilder.method,
                path,
                durationSeconds.toLong(),
                true,
                SigningLocation.HEADER
            )
        """.formatForTest("    ")

        actual.shouldContainOnlyOnceWithDiff(expected)
    }

    @Test
    fun testPresignOperationCodegenQuerystringTransform() {
        val testPresignerModel = setOf(
            PresignableOperation(
                "smithy.example#Example",
                "smithy.example#GetFoo",
                null,
                "HEADER",
                setOf(),
                null,
                false,
                true
            )
        )
        val unit = PresignerIntegration(testPresignerModel)

        unit.writeAdditionalFiles(codegenContext, testContext.generationCtx.delegator)

        val testManifest = testContext.generationCtx.delegator.fileManifest as MockManifest
        val actual = testManifest.expectFileString("/./src/main/kotlin/smithy/example/Presigner.kt")

        val substVal = "\${request.payload.toString().urlEncodeComponent()}"
        val expected = """
            private suspend fun getFooPresignConfig(request: GetFooRequest, durationSeconds: ULong) : PresignedRequestConfig {
                require(durationSeconds > 0u) { "duration must be greater than zero" }
                val execContext = ExecutionContext.build {  }
                val httpRequestBuilder = GetFooOperationSerializer().serialize(execContext, request)
                val queryStringBuilder = StringBuilder()
                queryStringBuilder.append(httpRequestBuilder.url.path)
                queryStringBuilder.append("?")
                if (request.payload != null) {
                    queryStringBuilder.append("Payload=$substVal&")
                }
                val path = queryStringBuilder.toString()
                return PresignedRequestConfig(
                    setOf(),
                    httpRequestBuilder.method,
                    path,
                    durationSeconds.toLong(),
                    false,
                    SigningLocation.HEADER
                )
            }
        """.trimIndent()

        actual.shouldContainOnlyOnceWithDiff(expected)
    }
}