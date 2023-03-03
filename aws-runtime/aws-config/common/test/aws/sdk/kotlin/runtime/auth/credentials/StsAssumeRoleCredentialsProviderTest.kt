/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.auth.credentials.internal.sts.model.RegionDisabledException
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProviderException
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.net.Host
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StsAssumeRoleCredentialsProviderTest {
    private val sourceProvider = StaticCredentialsProvider {
        accessKeyId = "AKID"
        secretAccessKey = "secret"
    }

    private val testArn = "arn:aws:iam:1234567/test-role"

    @Test
    fun testSuccess() = runTest {
        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            httpClientEngine = testEngine,
        )

        val actual = provider.resolve()
        assertEquals(StsTestUtils.expectedCredentialsBase, actual)
    }

    @Test
    fun testServiceFailure() = runTest {
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
            httpClientEngine = testEngine,
        )

        assertFailsWith<CredentialsProviderException> {
            provider.resolve()
        }.message.shouldContain("failed to assume role from STS")
    }

    @Test
    fun testRegionDisabled() = runTest {
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
            region = "us-west-2",
            httpClientEngine = testEngine,
        )

        val ex = assertFailsWith<ProviderConfigurationException> {
            provider.resolve()
        }

        ex.message.shouldContain("STS is not activated in the requested region (us-west-2). Please check your configuration and activate STS in the target region if necessary")
        assertIs<RegionDisabledException>(ex.cause)
    }

    @Test
    fun testGlobalEndpoint() = runTest {
        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            httpClientEngine = testEngine,
        )

        val actual = provider.resolve()
        assertEquals(StsTestUtils.expectedCredentialsBase, actual)
        val req = testEngine.requests().first()
        assertEquals(Host.Domain("sts.amazonaws.com"), req.actual.url.host)
    }

    @Test
    fun testRegionalEndpoint() = runTest {
        val testEngine = buildTestConnection {
            expect(StsTestUtils.stsResponse(testArn))
        }

        val provider = StsAssumeRoleCredentialsProvider(
            credentialsProvider = sourceProvider,
            roleArn = testArn,
            region = "us-west-2",
            httpClientEngine = testEngine,
        )

        val actual = provider.resolve()
        assertEquals(StsTestUtils.expectedCredentialsBase, actual)
        val req = testEngine.requests().first()
        assertEquals(Host.Domain("sts.us-west-2.amazonaws.com"), req.actual.url.host)
    }
}
