/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.sdk.kotlin.services.s3.putObject
import aws.sdk.kotlin.services.s3.withConfig
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

/**
 * Tests for S3 Express operations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ExpressTest {
    private val client = S3Client {
        region = S3TestUtils.DEFAULT_REGION
    }

    private val testBuckets: MutableList<String> = mutableListOf()

    @BeforeAll
    fun setup(): Unit = runBlocking {
        val suffix = "--usw2-az1--x-s3" // us-west-2 availability zone 1

        // create a few test buckets to test the credentials cache
        testBuckets.add(S3TestUtils.getTestDirectoryBucket(client, suffix))
        testBuckets.add(S3TestUtils.getTestDirectoryBucket(client, suffix))
        testBuckets.add(S3TestUtils.getTestDirectoryBucket(client, suffix))
    }

    @AfterAll
    fun cleanup(): Unit = runBlocking {
        testBuckets.forEach { bucket ->
            S3TestUtils.deleteBucketAndAllContents(client, bucket)
        }
        client.close()
    }

    @Test
    fun testPutObject() = runTest {
        val content = "30 minutes, or it's free!"
        val keyName = "express.txt"

        testBuckets.forEach { bucketName ->
            client.putObject {
                bucket = bucketName
                key = keyName
                body = ByteStream.fromString(content)
            }

            val req = GetObjectRequest {
                bucket = bucketName
                key = keyName
            }

            val respContent = client.getObject(req) {
                it.body?.decodeToString()
            }

            assertEquals(content, respContent)
        }
    }

    @Test
    fun testChecksums() = runTest {
        val bucketName = testBuckets.first() // only need one bucket for this test

        val keysToDelete = listOf("checksums.txt", "delete-me.txt", "dont-forget-about-me.txt")
        keysToDelete.forEach {
            client.putObject {
                bucket = bucketName
                key = it
                body = ByteStream.fromString("Check out these sums!")
            }
        }

        client.withConfig {
            interceptors += CRC32ChecksumValidatingInterceptor()
        }.use { validatingClient ->
            // s3:DeleteObjects requires a checksum, even if the user doesn't specify one.
            // normally the SDK would default to MD5, but S3 Express must default to CRC32 instead.
            val req = DeleteObjectsRequest {
                bucket = bucketName
                delete = Delete {
                    objects = keysToDelete.map {
                        ObjectIdentifier { key = it }
                    }
                }
            }

            validatingClient.deleteObjects(req)
        }
    }

    private class CRC32ChecksumValidatingInterceptor : HttpInterceptor {
        override fun readAfterSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
            val headers = context.protocolRequest.headers
            if (headers.contains("X-Amz-S3session-Token")) {
                assertTrue(headers.contains("x-amz-checksum-crc32"), "Failed to find x-amz-checksum-crc32 header")
                assertFalse(headers.contains("Content-MD5"), "Unexpectedly found Content-MD5 header")
            }
        }
    }
}
