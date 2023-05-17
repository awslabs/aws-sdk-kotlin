/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignPutObjectRequest
import aws.sdk.kotlin.testing.PRINTABLE_CHARS
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.toByteStream
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3PresignerTest {
    private val client = S3Client {
        region = S3TestUtils.DEFAULT_REGION
    }

    private lateinit var testBucket: String

    @BeforeAll
    fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.getTestBucket(client)
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        S3TestUtils.deleteBucketAndAllContents(client, testBucket)
        client.close()
    }

    @Test
    fun testPresign() = runBlocking {
        val contents = "presign-test"
        val keyName = "foo$PRINTABLE_CHARS"

        withAllEngines { engine ->
            val httpClient = SdkHttpClient(engine)

            val unsignedPutRequest = PutObjectRequest {
                bucket = testBucket
                key = keyName
            }
            val presignedPutRequest = client.presignPutObjectRequest(unsignedPutRequest, 60.seconds)

            S3TestUtils.responseCodeFromPut(presignedPutRequest, contents)

            val unsignedGetRequest = GetObjectRequest {
                bucket = testBucket
                key = keyName
            }
            val presignedGetRequest = client.presignGetObjectRequest(unsignedGetRequest, 60.seconds)

            val call = httpClient.call(presignedGetRequest)
            val body = call.response.body.toByteStream()?.decodeToString()
            call.complete()
            assertEquals(200, call.response.status.value)
            assertEquals(contents, body)
        }
    }
}
