package aws.sdk.kotlin.services.bedrock

import aws.sdk.kotlin.services.bedrock.auth.finalizeBearerTokenConfig
import aws.smithy.kotlin.runtime.auth.AuthSchemeId
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.auth.BearerToken
import aws.smithy.kotlin.runtime.http.auth.BearerTokenProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.TestPlatformProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BedrockEnvironmentBearerTokenTest {
    private fun mockHttpClient(handler: (HttpRequest) -> HttpResponse): HttpClientEngine {
        return object : HttpClientEngineBase("test engine") {
            override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                val response = handler(request)
                return HttpCall(request, response, Instant.now(), Instant.now())
            }
        }
    }

    private val mockPlatformProvider = TestPlatformProvider(
        env = mapOf("AWS_BEARER_TOKEN_BEDROCK" to "bedrock-token"),
    )

    fun testAuthSchemePreferenceConfigured() = runTest {
        val builder = BedrockClient.Builder()
        val expectedAuthSchemePreference = listOf(AuthSchemeId.HttpBearer)
        finalizeBearerTokenConfig(builder, mockPlatformProvider)
        assertEquals(expectedAuthSchemePreference, builder.config.authSchemePreference)
    }

    fun testBearerAuthSchemePromotedToFirst() = runTest {
        val builder = BedrockClient.Builder()
        builder.config.authSchemePreference = listOf(AuthSchemeId.AwsSigV4)
        finalizeBearerTokenConfig(builder, mockPlatformProvider)
        val expectedAuthSchemePreference = listOf(AuthSchemeId.HttpBearer, AuthSchemeId.AwsSigV4)
        assertEquals(expectedAuthSchemePreference, builder.config.authSchemePreference)

        builder.config.authSchemePreference = listOf(AuthSchemeId.AwsSigV4, AuthSchemeId.HttpBearer)
        assertEquals(expectedAuthSchemePreference, builder.config.authSchemePreference)
    }

    fun testBearerTokenProviderConfigured() = runTest {
        val builder = BedrockClient.Builder()
        finalizeBearerTokenConfig(builder, mockPlatformProvider)
        assertNotNull(builder.config.bearerTokenProvider)
        val token = builder.config.bearerTokenProvider!!.resolve()
        assertNotNull(token)
        assertEquals("bedrock-token", token.token)
    }

    fun testExplicitProviderTakesPrecedence() = runTest {
        val builder = BedrockClient.Builder()
        builder.config.bearerTokenProvider = object : BearerTokenProvider {
            override suspend fun resolve(attributes: Attributes): BearerToken = object : BearerToken {
                override val token: String = "different-bedrock-token"
                override val attributes: Attributes = emptyAttributes()
                override val expiration: Instant? = null
            }
        }
        finalizeBearerTokenConfig(builder, mockPlatformProvider)
        assertNotNull(builder.config.bearerTokenProvider)
        val token = builder.config.bearerTokenProvider!!.resolve()
        assertNotNull(token)
        assertEquals("different-bedrock-token", token.token)
    }

    fun testBearerTokenProviderFunctionality() = runTest {
        var capturedAuthHeader: String? = null

        BedrockClient.fromEnvironment {
            region = "us-west-2"
            httpClient = mockHttpClient { request ->
                // Capture the Authorization header
                capturedAuthHeader = request.headers["Authorization"]
                HttpResponse(
                    status = HttpStatusCode.OK,
                    headers = Headers.Empty,
                    body = HttpBody.Empty,
                )
            }
        }.use { client ->
            // Make an api call to capture Authorization header
            client.listFoundationModels()

            assertNotNull(capturedAuthHeader)
            assertEquals("Bearer bedrock-token", capturedAuthHeader)
        }
    }
}
