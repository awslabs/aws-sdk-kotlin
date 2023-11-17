/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.deleteObject
import aws.sdk.kotlin.services.s3.deleteObjects
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.util.Attributes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Handle200ErrorsInterceptorTest {

    object TestCredentialsProvider : CredentialsProvider {
        override suspend fun resolve(attributes: Attributes): Credentials = Credentials("AKID", "SECRET")
    }

    private fun newTestClient(): S3Client {
        val content = """
            <Error>
                <Code>SlowDown</Code>
                <Message>Please reduce your request rate.</Message>
                <RequestId>K2H6N7ZGQT6WHCEG</RequestId>
                <HostId>WWoZlnK4pTjKCYn6eNV7GgOurabfqLkjbSyqTvDMGBaI9uwzyNhSaDhOCPs8paFGye7S6b/AB3A=</HostId>
            </Error>
        """.trimIndent().encodeToByteArray()

        return S3Client {
            region = "us-east-1"
            credentialsProvider = TestCredentialsProvider
            retryStrategy {
                maxAttempts = 1
            }
            httpClient = buildTestConnection {
                expect(HttpResponse(HttpStatusCode.OK, body = HttpBody.fromBytes(content)))
            }
        }
    }

    fun assertException(ex: S3Exception) {
        val expectedMessage = "Please reduce your request rate."
        assertEquals("SlowDown", ex.sdkErrorMetadata.errorCode)
        assertEquals(expectedMessage, ex.sdkErrorMetadata.errorMessage)
        assertEquals(expectedMessage, ex.sdkErrorMetadata.errorMessage)
        assertEquals("K2H6N7ZGQT6WHCEG", ex.sdkErrorMetadata.requestId)
        assertEquals("WWoZlnK4pTjKCYn6eNV7GgOurabfqLkjbSyqTvDMGBaI9uwzyNhSaDhOCPs8paFGye7S6b/AB3A=", ex.sdkErrorMetadata.requestId2)
    }

    @Test
    fun testHandle200ErrorsWithNoExpectedBody() = runTest {
        val s3 = newTestClient()
        val ex = assertFailsWith<S3Exception> {
            s3.deleteObject { bucket = "test"; key = "key" }
        }
        assertException(ex)
    }

    @Test
    fun testHandle200ErrorsWithExpectedBody() = runTest {
        val s3 = newTestClient()
        val ex = assertFailsWith<S3Exception> {
            s3.deleteObjects { bucket = "test" }
        }
        assertException(ex)
    }
}
