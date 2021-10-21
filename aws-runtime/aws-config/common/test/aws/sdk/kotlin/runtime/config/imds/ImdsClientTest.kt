/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.operation.Endpoint
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ImdsClientTest {

    @Test
    fun testTokensAreCached() = runSuspendTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/metadata", "TOKEN_A"),
                imdsResponse("output 1")
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/metadata", "TOKEN_A"),
                imdsResponse("output 2")
            )
        }

        val client = ImdsClient { engine = connection }
        val r1 = client.get("/latest/metadata")
        assertEquals("output 1", r1)

        val r2 = client.get("/latest/metadata")
        assertEquals("output 2", r2)
        connection.assertRequests()
    }

    @Test
    fun testTokensCanExpire() = runSuspendTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://[fd00:ec2::254]", 600),
                tokenResponse(600, "TOKEN_A")
            )
            expect(
                imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_A"),
                imdsResponse("output 1")
            )
            expect(
                tokenRequest("http://[fd00:ec2::254]", 600),
                tokenResponse(600, "TOKEN_B")
            )
            expect(
                imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_B"),
                imdsResponse("output 2")
            )
        }

        val testClock = ManualClock()

        val client = ImdsClient {
            engine = connection
            endpointConfiguration = EndpointConfiguration.ModeOverride(EndpointMode.IPv6)
            clock = testClock
            tokenTtl = Duration.seconds(600)
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
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://[fd00:ec2::254]", 600),
                tokenResponse(600, "TOKEN_A")
            )
            // t = 0
            expect(
                imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_A"),
                imdsResponse("output 1")
            )
            // t = 400 (no refresh)
            expect(
                imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_A"),
                imdsResponse("output 2")
            )
            // t = 550 (within buffer)
            expect(
                tokenRequest("http://[fd00:ec2::254]", 600),
                tokenResponse(600, "TOKEN_B")
            )
            expect(
                imdsRequest("http://[fd00:ec2::254]/latest/metadata", "TOKEN_B"),
                imdsResponse("output 3")
            )
        }

        val testClock = ManualClock()

        val client = ImdsClient {
            engine = connection
            endpointConfiguration = EndpointConfiguration.ModeOverride(EndpointMode.IPv6)
            clock = testClock
            tokenTtl = Duration.seconds(600)
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

    @Ignore
    @Test
    fun testRetryHttp500() {
        fail("not implemented yet")
    }

    @Ignore
    @Test
    fun testRetryTokenFailure() {
        // 500 during token acquisition should be retried
        fail("not implemented yet")
    }

    @Ignore
    @Test
    fun testNoRetryHttp403() {
        // 403 responses from IMDS during token acquisition MUST not be retried
        fail("not implemented yet")
    }

    @Test
    fun testHttpConnectTimeouts(): Unit = runSuspendTest {
        // end-to-end real client times out after 1-second
        val client = ImdsClient {
            // will never resolve
            endpointConfiguration = EndpointConfiguration.Custom("http://240.0.0.0".toEndpoint())
        }

        val start = Instant.now()
        assertFails {
            withTimeout(3000) {
                client.get("/latest/metadata")
            }
        }.message.shouldContain("timed out")
        val elapsed = Instant.now().epochMilliseconds - start.epochMilliseconds
        assertTrue(elapsed >= 1000)
        assertTrue(elapsed < 2000)
    }

    data class ImdsConfigTest(
        val name: String,
        val env: Map<String, String>,
        val fs: Map<String, String>,
        val endpointOverride: String?,
        val modeOverride: String?,
        val result: Result<String>,
    ) {
        companion object {
            fun fromJson(element: JsonObject): ImdsConfigTest {
                val resultObj = element["result"]!!.jsonObject
                // map to success or generic error with the expected message substring of _some_ error that should be thrown
                val result = resultObj["Ok"]?.jsonPrimitive?.content?.let { Result.success(it) }
                    ?: resultObj["Err"]!!.jsonPrimitive.content.let { Result.failure(RuntimeException(it)) }

                return ImdsConfigTest(
                    element["name"]!!.jsonPrimitive.content,
                    element["env"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content },
                    element["fs"]!!.jsonObject.mapValues { it.value.jsonPrimitive.content },
                    element["endpointOverride"]?.jsonPrimitive?.content,
                    element["modeOverride"]?.jsonPrimitive?.content,
                    result
                )
            }
        }
    }

    @Test
    fun testConfig(): Unit = runSuspendTest {
        val tests = Json.parseToJsonElement(imdsTestSuite).jsonObject["tests"]!!.jsonArray.map { ImdsConfigTest.fromJson(it.jsonObject) }
        tests.forEach { test ->
            val result = runCatching { check(test) }
            when {
                test.result.isSuccess && result.isSuccess -> {}
                test.result.isSuccess && result.isFailure -> fail("expected success but failed; test=${test.name}; result=$result")
                test.result.isFailure && result.isSuccess -> fail("expected failure but succeeded; test=${test.name}; result=$result")
                test.result.isFailure && result.isFailure -> {
                    result.exceptionOrNull()!!.message.shouldContain(test.result.exceptionOrNull()!!.message!!)
                }
            }
        }
    }

    private suspend fun check(test: ImdsConfigTest) {
        val connection = buildTestConnection {
            if (test.result.isSuccess) {
                val endpoint = test.result.getOrThrow()
                expect(
                    tokenRequest(endpoint, DEFAULT_TOKEN_TTL_SECONDS),
                    tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
                )
                expect(imdsResponse("output 1"))
            }
        }

        val client = ImdsClient {
            engine = connection
            test.endpointOverride?.let { endpointOverride ->
                val endpoint = Endpoint(endpointOverride)
                endpointConfiguration = EndpointConfiguration.Custom(endpoint)
            }
            test.modeOverride?.let {
                endpointConfiguration = EndpointConfiguration.ModeOverride(EndpointMode.fromValue(it))
            }
            platformProvider = TestPlatformProvider(test.env, fs = test.fs)
        }

        client.get("/hello")
        connection.assertRequests()
    }
}
