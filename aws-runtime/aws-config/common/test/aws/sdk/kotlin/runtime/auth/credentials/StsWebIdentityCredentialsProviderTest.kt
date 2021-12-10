/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestConnection
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StsWebIdentityCredentialsProviderTest {

    private val epoch = Instant.fromIso8601("2020-10-16T03:56:00Z")
    private val expectedCredentialsBase = Credentials(
        "AKIDTest",
        "test-secret",
        "test-token",
        epoch + Duration.minutes(15)
    )

    private val testArn = "arn:aws:iam:1234567/test-role"

    // see https://docs.aws.amazon.com/STS/latest/APIReference/API_AssumeRoleWithWebIdentity.html#API_AssumeRoleWithWebIdentity_ResponseElements
    private fun sts_response(
        roleArn: String,
        expiration: Instant? = null
    ): HttpResponse {
        val roleId = roleArn.split("/").last()
        val expiry = expiration ?: expectedCredentialsBase.expiration!!
        val body = """
        <AssumeRoleWithWebIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
          <AssumeRoleWithWebIdentityResult>
            <SubjectFromWebIdentityToken>amzn1.account.AF6RHO7KZU5XRVQJGXK6HB56KR2A</SubjectFromWebIdentityToken>
            <Audience>client.5498841531868486423.1548@apps.example.com</Audience>
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
            <SourceIdentity>SourceIdentityValue</SourceIdentity>
            <Provider>www.amazon.com</Provider>
          </AssumeRoleWithWebIdentityResult>
          <ResponseMetadata>
            <RequestId>ad4156e9-bce1-11e2-82e6-6b6efEXAMPLE</RequestId>
          </ResponseMetadata>
        </AssumeRoleWithWebIdentityResponse>
        """.trimIndent()

        return HttpResponse(HttpStatusCode.OK, Headers.Empty, ByteArrayContent(body.encodeToByteArray()))
    }

    @Test
    fun testSuccess(): Unit = runSuspendTest {
        val testEngine = buildTestConnection {
            expect(sts_response(testArn))
        }

        val testPlatform = TestPlatformProvider(
            fs = mapOf("token-path" to "jwt-token")
        )

        val provider = StsWebIdentityCredentialsProvider(
            roleArn = testArn,
            webIdentityTokenFilePath = "token-path",
            region = "us-east-2",
            httpClientEngine = testEngine,
            platformProvider = testPlatform
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

        val testPlatform = TestPlatformProvider(
            fs = mapOf("token-path" to "jwt-token")
        )

        val provider = StsWebIdentityCredentialsProvider(
            roleArn = testArn,
            webIdentityTokenFilePath = "token-path",
            region = "us-east-2",
            httpClientEngine = testEngine,
            platformProvider = testPlatform
        )

        assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }.message.shouldContain("STS failed to assume role from web identity")
    }

    @Test
    fun testJwtTokenMissing(): Unit = runSuspendTest {
        val testEngine = TestConnection()

        val testPlatform = TestPlatformProvider()

        val provider = StsWebIdentityCredentialsProvider(
            roleArn = testArn,
            webIdentityTokenFilePath = "token-path",
            region = "us-east-2",
            httpClientEngine = testEngine,
            platformProvider = testPlatform
        )

        assertFailsWith<CredentialsProviderException> {
            provider.getCredentials()
        }.message.shouldContain("failed to read webIdentityToken from token-path")
    }

    @Test
    fun testFromEnvironment() {
        val tp1 = TestPlatformProvider(
            env = mapOf(
                "AWS_ROLE_ARN" to "my-role",
                "AWS_WEB_IDENTITY_TOKEN_FILE" to "token-file-path",
                "AWS_REGION" to "us-east-2"
            )
        )

        StsWebIdentityCredentialsProvider.fromEnvironment(platformProvider = tp1)

        // missing AWS_ROLE_ARN
        assertFailsWith<ProviderConfigurationException> {
            val tp2 = TestPlatformProvider(
                env = mapOf(
                    "AWS_WEB_IDENTITY_TOKEN_FILE" to "token-file-path",
                    "AWS_REGION" to "us-east-2"
                )
            )
            StsWebIdentityCredentialsProvider.fromEnvironment(platformProvider = tp2)
        }.message.shouldContain("Required field `roleArn` could not be automatically inferred for StsWebIdentityCredentialsProvider. Either explicitly pass a value, set the environment variable `AWS_ROLE_ARN`, or set the JVM system property `aws.roleArn`")
    }
}
