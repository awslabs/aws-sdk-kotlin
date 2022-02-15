/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Tests for bucket operations and presigner
 */
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3BucketOpsIntegrationTest {
    companion object {
        const val DEFAULT_REGION = "us-east-2"
    }

    private val client = S3Client {
        region = DEFAULT_REGION
    }

    private lateinit var testBucket: String

    @BeforeAll
    private fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.getTestBucket(client)
    }

    @AfterAll
    private fun cleanup() = runBlocking {
        S3TestUtils.deleteBucketAndAllContents(client, testBucket)
    }

    @Test
    fun testPutObjectFromMemory(): Unit = runBlocking {
        val contents = """
            A lep is a ball.
            A tay is a hammer.
            A korf is a tiger.
            A flix is a comb.
            A wogsin is a gift.
        """.trimIndent()

        val keyName = "put-obj-from-memory.txt"

        client.putObject {
            bucket = testBucket
            key = keyName
            body = ByteStream.fromString(contents)
        }

        val req = GetObjectRequest {
            bucket = testBucket
            key = keyName
        }
        val roundTrippedContents = client.getObject(req) { it.body?.decodeToString() }

        assertEquals(contents, roundTrippedContents)
    }

    @Test
    fun testPutObjectFromFile(): Unit = runBlocking {
        val tempFile = RandomTempFile(1024)
        val keyName = "put-obj-from-file.txt"

        // This test fails sporadically (by never completing)
        // see https://github.com/awslabs/aws-sdk-kotlin/issues/282
        withTimeout(5.seconds) {
            client.putObject {
                bucket = testBucket
                key = keyName
                body = ByteStream.fromFile(tempFile)
            }
        }

        val req = GetObjectRequest {
            bucket = testBucket
            key = keyName
        }
        val roundTrippedContents = client.getObject(req) { it.body?.decodeToString() }

        val contents = tempFile.readText()
        assertEquals(contents, roundTrippedContents)
    }

    @Test
    fun testListObjectsWithDelimiter(): Unit = runBlocking {
        // see: https://github.com/awslabs/aws-sdk-kotlin/issues/448

        client.listObjects {
            bucket = testBucket
            delimiter = "/"
            prefix = null
        }

        // only care that request is accepted, not the results
    }
}
