/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.Protocol
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import io.kotest.matchers.string.shouldContain
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ImdsCredentialsProviderTest {

    @Test
    fun testImdsDisabled() = runTest {
        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsEc2MetadataDisabled.environmentVariable to "true"),
        )
        val provider = ImdsCredentialsProvider(platformProvider = platform)
        assertFailsWith<CredentialsNotLoadedException> {
            provider.getCredentials()
        }.message.shouldContain("AWS EC2 metadata is explicitly disabled; credentials not loaded")
    }

    @Test
    fun testSuccess() = runTest {
        val testClock = ManualClock()
        val expiration: Instant = testClock.now() + 5.minutes

        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials", "TOKEN_A"),
                imdsResponse("imds-test-role"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role", "TOKEN_A"),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "$expiration"
                    }
                """,
                ),
            )
        }

        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(client = lazyOf(client))

        val actual = provider.getCredentials()
        val expected = Credentials(
            "ASIARTEST",
            "xjtest",
            "IQote///test",
            expiration,
            "IMDSv2",
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testSuccessProfileOverride() = runTest {
        val testClock = ManualClock()
        val expiration = testClock.now() + 5.minutes

        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            // no request for profile, go directly to retrieving role credentials
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role", "TOKEN_A"),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "$expiration"
                    }
                """,
                ),
            )
        }

        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(profileOverride = "imds-test-role", client = lazyOf(client))

        val actual = provider.getCredentials()
        val expected = Credentials(
            "ASIARTEST",
            "xjtest",
            "IQote///test",
            expiration,
            "IMDSv2",
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testTokenFailure() = runTest {
        // when attempting to retrieve initial token, IMDS replied with 403, indicating IMDS is disabled or not allowed through permissions
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                HttpResponse(HttpStatusCode.Forbidden, Headers.Empty, HttpBody.Empty),
            )
        }

        val testClock = ManualClock()
        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(client = lazyOf(client))

        val ex = assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }
        ex.message.shouldContain("failed to load instance profile")
        assertIs<EC2MetadataError>(ex.cause)
        ex.cause!!.message.shouldContain("Request forbidden")
    }

    @Test
    fun testNoInstanceProfileConfigured() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials", "TOKEN_A"),
                HttpResponse(
                    HttpStatusCode.NotFound,
                    Headers.Empty,
                    ByteArrayContent(
                        """<?xml version="1.0" encoding="iso-8859-1"?>
                        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
                                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
                         <head>
                          <title>404 - Not Found</title>
                         </head>
                         <body>
                          <h1>404 - Not Found</h1>
                         </body>
                        </html>
                        """.trimIndent().encodeToByteArray(),
                    ),
                ),
            )
        }

        val testClock = ManualClock()
        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(client = lazyOf(client))

        assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }.message.shouldContain("failed to load instance profile")
    }

    // SDK can send a request if expired credentials are available.
    // If the credentials provider can return expired credentials, that means the SDK can use them,
    // because no other checks are done before using the credentials.
    @Test
    fun testCanReturnExpiredCredentials() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role", "TOKEN_A"),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "2021-09-18T03:31:56Z"
                    }
                """,
                ),
            )
        }

        val testClock = ManualClock()
        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
        )

        val actual = provider.getCredentials()

        val expected = Credentials(
            accessKeyId = "ASIARTEST",
            secretAccessKey = "xjtest",
            sessionToken = "IQote///test",
            expiration = Instant.fromEpochSeconds(1631935916),
            providerName = "IMDSv2",
            nextRefresh = testClock.now() + DEFAULT_CREDENTIALS_REFRESH_SECONDS.seconds, // 15 minutes
        )

        assertEquals(expected, actual)
    }

    // SDK can perform 3 successive requests with expired credentials. IMDS must only be called once.
    @Test
    fun testSuccessiveRequestsOnlyCallIMDSOnce() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role", "TOKEN_A"),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "2021-09-18T03:31:56Z"
                    }
                """,
                ),
            )
        }

        val testClock = ManualClock()

        val client = spyk(
            ImdsClient {
                engine = connection
                clock = testClock
            },
        )

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
        )

        // call getCredentials 3 times
        for (i in 1..3) {
            provider.getCredentials()
        }

        // make sure ImdsClient only gets called once
        coVerify(exactly = 1) {
            client.get(any())
        }
    }

    @Test
    fun testDontRefreshUntilNextRefreshTimeHasPassed() = runTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A"),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role", "TOKEN_A"),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "ASIARTEST",
                        "SecretAccessKey" : "xjtest",
                        "Token" : "IQote///test",
                        "Expiration" : "2021-09-18T03:31:56Z"
                    }
                """,
                ),
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials/imds-test-role", "TOKEN_A"),
                imdsResponse(
                    """
                    {
                        "Code" : "Success",
                        "LastUpdated" : "2021-09-17T20:57:08Z",
                        "Type" : "AWS-HMAC",
                        "AccessKeyId" : "NEWCREDENTIALS",
                        "SecretAccessKey" : "shhh",
                        "Token" : "IQote///test",
                        "Expiration" : "2022-10-05T03:31:56Z"
                    }
                """,
                ),
            )
        }

        val testClock = ManualClock()

        val client = spyk(
            ImdsClient {
                engine = connection
                clock = testClock
            },
        )

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
        )

        val first = provider.getCredentials()
        testClock.advance(20.minutes) // 20 minutes later, we should try to refresh the expired credentials
        val second = provider.getCredentials()

        coVerify(exactly = 2) {
            client.get(any())
        }

        // make sure we did not just serve the previous credentials
        assertNotEquals(first, second)
    }

    @Test
    fun testUsesPreviousCredentialsOnReadTimeout() = runTest {
        // this engine throws read timeout exceptions
        val readTimeoutEngine = object : HttpClientEngineBase("readTimeout") {
            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                throw SocketTimeoutException()
            }
        }

        val testClock = ManualClock()

        val client = ImdsClient {
            engine = readTimeoutEngine
            clock = testClock
        }

        var previousCredentials = Credentials(
            accessKeyId = "ASIARTEST",
            secretAccessKey = "xjtest",
            sessionToken = "IQote///test",
            expiration = Instant.fromEpochSeconds(1631935916),
            providerName = "IMDSv2",
            nextRefresh = testClock.now() - 1.seconds, // nextRefresh is NOW! this will guarantee we call IMDS the next time around
        )

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            previousCredentials = previousCredentials, // pretend that we served these credentials previously
        )

        val credentials = provider.getCredentials()

        // copy the new credentials' nextRefresh value, since it will be updated, and we only want to compare the other fields
        previousCredentials = previousCredentials.copy(nextRefresh = credentials.nextRefresh)

        // a read timeout should cause provider to return the previously-served credentials with an updated nextRefresh time
        assertEquals(credentials, previousCredentials)
    }

    @Test
    fun testThrowsExceptionOnReadTimeoutWhenMissingPreviousCredentials() = runTest {
        val readTimeoutEngine = object : HttpClientEngineBase("readTimeout") {
            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                throw SocketTimeoutException()
            }
        }

        val testClock = ManualClock()

        val client = ImdsClient {
            engine = readTimeoutEngine
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
        )

        // a read timeout should cause an exception to be thrown, because we have no previous credentials to re-serve
        assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }
    }

    @Test
    fun testUsesPreviousCredentialsOnServerError() = runTest {
        val testClock = ManualClock()

        // this engine just returns 500 errors
        val internalServerErrorEngine = object : HttpClientEngineBase("readTimeout") {
            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                return HttpCall(
                    HttpRequest(HttpMethod.GET, Url(Protocol.HTTP, "test", Protocol.HTTP.defaultPort, "/path/foo/bar"), Headers.Empty, HttpBody.Empty),
                    HttpResponse(HttpStatusCode.InternalServerError, Headers.Empty, HttpBody.Empty),
                    testClock.now(),
                    testClock.now(),
                )
            }
        }

        val client = ImdsClient {
            engine = internalServerErrorEngine
            clock = testClock
        }

        var previousCredentials = Credentials(
            accessKeyId = "ASIARTEST",
            secretAccessKey = "xjtest",
            sessionToken = "IQote///test",
            expiration = Instant.fromEpochSeconds(1631935916),
            providerName = "IMDSv2",
            nextRefresh = testClock.now() - 1.seconds, // nextRefresh is NOW! this will guarantee we call IMDS the next time around
        )

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
            previousCredentials = previousCredentials, // pretend that we served these credentials previously
        )

        val credentials = provider.getCredentials()

        // copy the new credentials' nextRefresh value, since it will be updated, and we only want to compare the other fields
        previousCredentials = previousCredentials.copy(nextRefresh = credentials.nextRefresh)

        // a 500 error should cause provider to return the previously-served credentials with an updated nextRefresh time
        assertEquals(credentials, previousCredentials)
    }

    @Test
    fun testThrowsExceptionOnServerErrorWhenMissingPreviousCredentials() = runTest {
        val testClock = ManualClock()

        // this engine just returns 500 errors
        val internalServerErrorEngine = object : HttpClientEngineBase("readTimeout") {
            override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
                return HttpCall(
                    HttpRequest(HttpMethod.GET, Url(Protocol.HTTP, "test", Protocol.HTTP.defaultPort, "/path/foo/bar"), Headers.Empty, HttpBody.Empty),
                    HttpResponse(HttpStatusCode.InternalServerError, Headers.Empty, HttpBody.Empty),
                    testClock.now(),
                    testClock.now(),
                )
            }
        }

        val client = ImdsClient {
            engine = internalServerErrorEngine
            clock = testClock
        }

        val provider = ImdsCredentialsProvider(
            profileOverride = "imds-test-role",
            client = lazyOf(client),
            clock = testClock,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }
    }
}
