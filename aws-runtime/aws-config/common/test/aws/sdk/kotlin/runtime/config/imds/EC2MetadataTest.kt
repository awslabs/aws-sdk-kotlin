/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.sdk.kotlin.runtime.testing.ManualClock
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.callContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.url
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EC2MetadataTest {

    // TODO - move to shared test utils
    data class ExpectedHttpRequest(val request: HttpRequest, val response: HttpResponse? = null)
    data class ValidateRequest(val expected: HttpRequest, val actual: HttpRequest) {
        fun assertRequest() = runSuspendTest {
            assertEquals(expected.url.toString(), actual.url.toString(), "URL mismatch")
            expected.headers.forEach { name, values ->
                values.forEach {
                    assertTrue(actual.headers.contains(name, it), "Header $name missing value $it")
                }
            }

            val expectedBody = expected.body.readAll()?.decodeToString()
            val actualBody = actual.body.readAll()?.decodeToString()
            assertEquals(expectedBody, actualBody, "ValidateRequest HttpBody mismatch")
        }
    }

    class TestConnection(expected: List<ExpectedHttpRequest> = emptyList()) : HttpClientEngineBase("TestConnection") {
        private val expected = expected.toMutableList()
        // expected is mutated in-flight, store original size
        private val expectedCount = expected.size
        private var captured = mutableListOf<ValidateRequest>()

        override suspend fun roundTrip(request: HttpRequest): HttpCall {
            val next = expected.removeFirstOrNull() ?: error("TestConnection has no remaining expected requests")
            captured.add(ValidateRequest(next.request, request))

            val response = next.response ?: HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
            val now = Instant.now()
            return HttpCall(request, response, now, now, callContext())
        }

        fun requests(): List<ValidateRequest> = captured
        fun assertRequests() {
            assertEquals(expectedCount, captured.size)
            captured.forEach(ValidateRequest::assertRequest)
        }
    }

    private fun tokenRequest(host: String, ttl: Int): HttpRequest = HttpRequest {
        val parsed = Url.parse(host)
        url(parsed)
        url.path = "/latest/api/token"
        headers.append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.toString())
    }

    private fun tokenResponse(ttl: Int, token: String): HttpResponse = HttpResponse(
        HttpStatusCode.OK,
        Headers {
            append(X_AWS_EC2_METADATA_TOKEN_TTL_SECONDS, ttl.toString())
        },
        ByteArrayContent(token.encodeToByteArray())
    )

    private fun imdsRequest(url: String, token: String): HttpRequest = HttpRequest {
        val parsed = Url.parse(url)
        url(parsed)
        headers.append(X_AWS_EC2_METADATA_TOKEN, token)
    }

    private fun imdsResponse(body: String): HttpResponse = HttpResponse(
        HttpStatusCode.OK,
        Headers.Empty,
        ByteArrayContent(body.encodeToByteArray())
    )

    @Test
    fun testInvalidEndpointOverrideFailsCreation() {
        val connection = TestConnection()
        assertFailsWith<ConfigurationException> {
            EC2Metadata {
                engine = connection
                endpoint = Endpoint("[foo::254]", protocol = "http")
            }
        }
    }

    @Test
    fun testTokensAreCached() = runSuspendTest {
        val connection = TestConnection(
            listOf(
                ExpectedHttpRequest(
                    tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                    tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
                ),
                ExpectedHttpRequest(
                    imdsRequest("http://169.254.169.254/latest/metadata", "TOKEN_A"),
                    imdsResponse("output 1")
                ),
                ExpectedHttpRequest(
                    imdsRequest("http://169.254.169.254/latest/metadata", "TOKEN_A"),
                    imdsResponse("output 2")
                )
            )
        )

        val client = EC2Metadata { engine = connection }
        val r1 = client.get("/latest/metadata")
        assertEquals("output 1", r1)

        val r2 = client.get("/latest/metadata")
        assertEquals("output 2", r2)
        connection.assertRequests()
    }

    @Test
    fun testTokensCanExpire() = runSuspendTest {
        val connection = TestConnection(
            listOf(
                ExpectedHttpRequest(
                    tokenRequest("http://[fd00:ec2::254]", 600),
                    tokenResponse(600, "TOKEN_A")
                ),
                ExpectedHttpRequest(
                    imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_A"),
                    imdsResponse("output 1")
                ),
                ExpectedHttpRequest(
                    tokenRequest("http://[fd00:ec2::254]", 600),
                    tokenResponse(600, "TOKEN_B")
                ),
                ExpectedHttpRequest(
                    imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_B"),
                    imdsResponse("output 2")
                )
            )
        )

        val testClock = ManualClock()

        val client = EC2Metadata {
            engine = connection
            endpointMode = EndpointMode.IPv6
            clock = testClock
            tokenTTL = Duration.seconds(600)
        }

        val r1 = client.get("/latest/metadata")
        assertEquals("output 1", r1)
        testClock.advance(Duration.seconds(600))

        val r2 = client.get("/latest/metadata")
        assertEquals("output 2", r2)
        connection.assertRequests()
    }

    @Test
    fun testTokenRefreshBuffer() = runSuspendTest {
        // tokens are refreshed up to 120 seconds early to avoid using an expired token
        val connection = TestConnection(
            listOf(
                ExpectedHttpRequest(
                    tokenRequest("http://[fd00:ec2::254]", 600),
                    tokenResponse(600, "TOKEN_A")
                ),
                // t = 0
                ExpectedHttpRequest(
                    imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_A"),
                    imdsResponse("output 1")
                ),
                // t = 400 (no refresh)
                ExpectedHttpRequest(
                    imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_A"),
                    imdsResponse("output 2")
                ),
                // t = 550 (within buffer)
                ExpectedHttpRequest(
                    tokenRequest("http://[fd00:ec2::254]", 600),
                    tokenResponse(600, "TOKEN_B")
                ),
                ExpectedHttpRequest(
                    imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_B"),
                    imdsResponse("output 3")
                )
            )
        )

        val testClock = ManualClock()

        val client = EC2Metadata {
            engine = connection
            endpointMode = EndpointMode.IPv6
            clock = testClock
            tokenTTL = Duration.seconds(600)
        }

        val r1 = client.get("/latest/metadata")
        assertEquals("output 1", r1)
        testClock.advance(Duration.seconds(400))

        val r2 = client.get("/latest/metadata")
        assertEquals("output 2", r2)

        testClock.advance(Duration.seconds(150))
        val r3 = client.get("/latest/metadata")
        assertEquals("output 3", r3)

        connection.assertRequests()
    }

    @Test
    fun testRetryHttp500() {
        fail("not implemented yet")
    }

    @Test
    fun testRetryTokenFailure() {
        // 500 during token acquisition should be retried
        fail("not implemented yet")
    }

    @Test
    fun testNoRetryHttp403() {
        // 403 responses from IMDS during token acquisition MUST not be retried
        fail("not implemented yet")
    }

    @Test
    fun testConfig() {
        // need to mock various config scenarios
        fail("not implemented yet")
    }
}
