/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.RegionDisabledException
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StsAssumeRoleCredentialsProviderTest {
    private val sourceProvider = StaticCredentialsProvider {
        accessKeyId = "AKID"
        secretAccessKey = "secret"
    }

    private val epoch = Instant.fromIso8601("2020-10-16T03:56:00Z")
    private val expectedCredentialsBase = Credentials(
        "AKIDTest",
        "test-secret",
        "test-token",
        epoch + Duration.minutes(15)
    )

    private val testArn = "arn:aws:iam:1234567/test-role"

    // see https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRole.html#API_AssumeRole_ResponseElements
    private fun sts_response(
        roleArn: String,
        expiration: Instant? = null
    ): HttpResponse {
        val roleId = roleArn.split("/").last()
        val expiry = expiration ?: expectedCredentialsBase.expiration!!
        val body = """
            <AssumeRoleResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
              <AssumeRoleResult>
              <SourceIdentity>Alice</SourceIdentity>
                <AssumedRoleUser>
                  <Arn>$roleArn</Arn>
                  <AssumedRoleId>ARO123EXAMPLE123:$roleId</AssumedRoleId>
                </AssumedRoleUser>
                <Credentials>
                  <AccessKeyId>AKIDTest</AccessKeyId>
                  <SecretAccessKey>test-secret</SecretAccessKey>
                  <SessionToken>test-token</SessionToken>
                  <Expiration>${expiry.format(TimestampFormat.ISO_8601)}</Expiration>
                </Credentials>
                <PackedPolicySize>6</PackedPolicySize>
              </AssumeRoleResult>
              <ResponseMetadata>
                <RequestId>c6104cbe-af31-11e0-8154-cbc7ccf896c7</RequestId>
              </ResponseMetadata>
            </AssumeRoleResponse>
        """.trimIndent()

        return HttpResponse(HttpStatusCode.OK, Headers.Empty, ByteArrayContent(body.encodeToByteArray()))
    }

    @Test
    fun testSuccess(): Unit = runSuspendTest {
        val testEngine = buildTestConnection {
            expect(sts_response(testArn))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            httpClientEngine = testEngine
        )

        val actual = provider.getCredentials()
        assertEquals(expectedCredentialsBase, actual)
    }

    @Test
    fun testServiceFailure(): Unit = runSuspendTest {
        val errorResponseBody = """
        <ErrorResponse>
            <Error>
                <Type>Sender</Type>
                <Code>AccessDeniedException</Code>
                <Message>You do not have sufficient access to perform this action</Message>
            </Error>
            <RequestId>foo-id</RequestId>
        </ErrorResponse>
        """
        val testEngine = buildTestConnection {
            expect(HttpResponse(HttpStatusCode.BadRequest, Headers.Empty, ByteArrayContent(errorResponseBody.encodeToByteArray())))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            httpClientEngine = testEngine
        )

        assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }.message.shouldContain("failed to assume role from STS")
    }

    @Test
    fun testRegionDisabled(): Unit = runSuspendTest {
        val errorResponseBody = """
        <ErrorResponse>
            <Error>
                <Type>Sender</Type>
                <Code>RegionDisabledException</Code>
                <Message>AWS STS is not activated in the requested region for the account that is being asked to generate credentials</Message>
            </Error>
            <RequestId>foo-id</RequestId>
        </ErrorResponse>
        """
        val testEngine = buildTestConnection {
            expect(HttpResponse(HttpStatusCode.Forbidden, Headers.Empty, ByteArrayContent(errorResponseBody.encodeToByteArray())))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            httpClientEngine = testEngine
        )

        val ex = assertFailsWith<ProviderConfigurationException> {
            provider.getCredentials()
        }

        ex.message.shouldContain("STS is not activated in the requested region. Please check your configuration and activate STS in the target region if necessary")
        assertIs<RegionDisabledException>(ex.cause)
    }

    @Test
    fun testGlobalEndpoint(): Unit = runSuspendTest {
        val testEngine = buildTestConnection {
            expect(sts_response(testArn))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            httpClientEngine = testEngine
        )

        val actual = provider.getCredentials()
        assertEquals(expectedCredentialsBase, actual)
        val req = testEngine.requests().first()
        assertEquals("sts.amazonaws.com", req.actual.url.host)
    }

    @Test
    fun testRegionalEndpoint(): Unit = runSuspendTest {
        val testEngine = buildTestConnection {
            expect(sts_response(testArn))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            region = "us-west-2",
            httpClientEngine = testEngine
        )

        val actual = provider.getCredentials()
        assertEquals(expectedCredentialsBase, actual)
        val req = testEngine.requests().first()
        assertEquals("sts.us-west-2.amazonaws.com", req.actual.url.host)
    }
}
