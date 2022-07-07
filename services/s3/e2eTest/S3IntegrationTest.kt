/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CompletedPart
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.testing.PRINTABLE_CHARS
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.content.fromFile
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.hashing.sha256
import aws.smithy.kotlin.runtime.testing.RandomTempFile
import aws.smithy.kotlin.runtime.util.encodeToHex
import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.UUID
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
    fun testQueryParameterEncoding(): Unit = runBlocking {
        // see: https://github.com/awslabs/aws-sdk-kotlin/issues/448

        // this is mostly a stress test of signing w.r.t query parameter encoding (since
        // delimiter is bound via @httpQuery) and the ability of an HTTP engine to keep
        // the same encoding going out on the wire (e.g. not double percent encoding)

        s3WithAllEngines { s3 ->
            s3.listObjects {
                bucket = testBucket
                delimiter = PRINTABLE_CHARS
                prefix = null
            }
            // only care that request is accepted, not the results
        }
    }

    @Test
    fun testPathEncoding(): Unit = runBlocking {
        // this is mostly a stress test of signing w.r.t path encoding (since key is bound
        // via @httpLabel) and the ability of an HTTP engine to keep the same encoding going
        // out on the wire (e.g. not double percent encoding)

        // NOTE: S3 provides guidance on choosing object key names: https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html
        // This test includes all printable chars (including ones S3 recommends avoiding). Users should
        // strive to fall within the guidelines given by S3 though

        s3WithAllEngines { s3 ->
            val objKey = "foo$PRINTABLE_CHARS"
            val content = "hello rfc3986"

            s3.putObject {
                bucket = testBucket
                key = objKey
                body = ByteStream.fromString(content)
            }

            val req = GetObjectRequest {
                bucket = testBucket
                key = objKey
            }

            s3.getObject(req) { resp ->
                val actual = resp.body!!.decodeToString()
                assertEquals(content, actual)
            }
        }
    }

    @Test
    fun testMultipartUpload(): Unit = runBlocking {
        s3WithAllEngines { s3 ->
            val objKey = "test-multipart-${UUID.randomUUID()}"
            val contentSize: Long = 8 * 1024 * 1024 // 2 parts
            val file = RandomTempFile(sizeInBytes = contentSize)
            val partSize = 5 * 1024 * 1024 // 5 MB - min part size

            val expectedSha256 = file.readBytes().sha256().encodeToHex()

            val resp = s3.createMultipartUpload {
                bucket = testBucket
                key = objKey
            }

            val completedParts = file.chunk(partSize)
                .mapIndexed { idx, chunk ->
                    async {
                        val uploadResp = s3.uploadPart {
                            bucket = testBucket
                            key = objKey
                            uploadId = resp.uploadId
                            body = file.asByteStream(chunk)
                            partNumber = idx + 1
                        }

                        CompletedPart {
                            partNumber = idx + 1
                            eTag = uploadResp.eTag
                        }
                    }
                }
                .toList()
                .awaitAll()

            s3.completeMultipartUpload {
                bucket = testBucket
                key = objKey
                uploadId = resp.uploadId
                multipartUpload {
                    parts = completedParts
                }
            }

            // TOOD - eventually make use of s3 checksums
            val getRequest = GetObjectRequest {
                bucket = testBucket
                key = objKey
            }
            val actualSha256 = s3.getObject(getRequest) { resp ->
                resp.body!!.toByteArray().sha256().encodeToHex()
            }

            assertEquals(expectedSha256, actualSha256)
        }
    }
}

// generate sequence of "chunks" where each range defines the inclusive start and end bytes
private fun File.chunk(partSize: Int): Sequence<LongRange> =
    (0 until length() step partSize.toLong()).asSequence().map {
        it until minOf(it + partSize, length())
    }

internal suspend fun s3WithAllEngines(block: suspend (S3Client) -> Unit) {
    withAllEngines { engine ->
        S3Client {
            region = S3BucketOpsIntegrationTest.DEFAULT_REGION
            httpClientEngine = engine
        }.use {
            try {
                block(it)
            } catch (ex: Exception) {
                println("test failed for engine $engine")
                throw ex
            }
        }
    }
}
