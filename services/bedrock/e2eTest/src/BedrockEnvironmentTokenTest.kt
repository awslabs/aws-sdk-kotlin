/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.bedrock.e2etest

import aws.sdk.kotlin.services.bedrock.BedrockClient
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
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

// Environment variable AWS_BEARER_TOKEN_BEDROCK is configured with the value "bedrock-token" for this test suite.
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BedrockEnvironmentTokenTest {
    private fun mockHttpClient(handler: (HttpRequest) -> HttpResponse): HttpClientEngine {
        return object : HttpClientEngineBase("test engine") {
            override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                val response = handler(request)
                return HttpCall(request, response, Instant.now(), Instant.now())
            }
        }
    }

    @Test
    fun testAuthSchemePreferenceConfigured(): Unit = runBlocking {
        val client = BedrockClient.fromEnvironment {
            region = "us-west-2"
        }

        val expectedAuthSchemePreference = listOf(AuthSchemeId.HttpBearer)
        assertEquals(expectedAuthSchemePreference, client.config.authSchemePreference)

        client.close()
    }

    @Test
    fun testBearerAuthSchemePromotedToFirst(): Unit = runBlocking {
        var client = BedrockClient.fromEnvironment {
            region = "us-west-2"
            authSchemePreference = listOf(AuthSchemeId.AwsSigV4)
        }

        var expectedAuthSchemePreference = listOf(AuthSchemeId.HttpBearer, AuthSchemeId.AwsSigV4)
        assertEquals(expectedAuthSchemePreference, client.config.authSchemePreference)

        client = BedrockClient.fromEnvironment {
            region = "us-west-2"
            authSchemePreference = listOf(AuthSchemeId.AwsSigV4, AuthSchemeId.HttpBearer)
        }

        expectedAuthSchemePreference = listOf(AuthSchemeId.HttpBearer, AuthSchemeId.AwsSigV4)
        assertEquals(expectedAuthSchemePreference, client.config.authSchemePreference)

        client.close()
    }

    @Test
    fun testBearerTokenProviderConfigured(): Unit = runBlocking {
        val client = BedrockClient.fromEnvironment {
            region = "us-west-2"
        }

        assertNotNull(client.config.bearerTokenProvider)

        val token = client.config.bearerTokenProvider.resolve()
        assertNotNull(token)
        assertEquals("bedrock-token", token.token)

        client.close()
    }

    @Test
    fun testBearerTokenProviderFunctionality(): Unit = runBlocking {
        var capturedAuthHeader: String? = null

        val client = BedrockClient.fromEnvironment {
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
        }

        // Make an api call to capture Authorization header
        client.listFoundationModels()

        assertNotNull(capturedAuthHeader)
        assertEquals("Bearer bedrock-token", capturedAuthHeader)

        client.close()
    }

    @Test
    fun testExplicitProviderTakesPrecedence(): Unit = runBlocking {
        val client = BedrockClient.fromEnvironment {
            region = "us-west-2"
            bearerTokenProvider = object : BearerTokenProvider {
                override suspend fun resolve(attributes: Attributes): BearerToken {
                    return object : BearerToken {
                        override val token: String = "different-bedrock-token"
                        override val attributes: Attributes = emptyAttributes()
                        override val expiration: Instant? = null
                    }
                }
            }
        }

        assertNotNull(client.config.bearerTokenProvider)

        val token = client.config.bearerTokenProvider.resolve()
        assertNotNull(token)
        assertEquals("different-bedrock-token", token.token)
    }
}
