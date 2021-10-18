/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ImdsCredentialsProviderTest {

    @Test
    fun testImdsDisabled(): Unit = runSuspendTest {
        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsEc2MetadataDisabled.environmentVariable to "true")
        )
        val provider = ImdsCredentialsProvider(platformProvider = platform)
        assertFailsWith<CredentialsNotLoadedException> {
            provider.getCredentials()
        }.message.shouldContain("AWS EC2 metadata is explicitly disabled; credentials not loaded")
    }

    @Test
    fun testSuccess(): Unit = runSuspendTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/iam/security-credentials", "TOKEN_A"),
                imdsResponse("imds-test-role")
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
                """
                )
            )
        }

        val testClock = ManualClock()
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
            Instant.fromEpochSeconds(1631935916)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testSuccessProfileOverride(): Unit = runSuspendTest {
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
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
                        "Expiration" : "2021-09-18T03:31:56Z"
                    }
                """
                )
            )
        }

        val testClock = ManualClock()
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
            Instant.fromEpochSeconds(1631935916)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun testTokenFailure(): Unit = runSuspendTest {
        // when attempting to retrieve initial token, IMDS replied with 403, indicating IMDS is disabled or not allowed through permissions
        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                HttpResponse(HttpStatusCode.Forbidden, Headers.Empty, HttpBody.Empty)
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
    fun testNoInstanceProfileConfigured(): Unit = runSuspendTest {

        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
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
                        """.trimIndent().encodeToByteArray()
                    )
                )
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
}
