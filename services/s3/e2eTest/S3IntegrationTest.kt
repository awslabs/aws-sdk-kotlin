/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.runtime.testing.RandomTempFile
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import aws.smithy.kotlin.runtime.content.fromFile
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Tests for bucket operations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3BucketOpsIntegrationTest {
    companion object {
        const val DEFAULT_REGION = "us-east-2"
    }

    val client = S3Client {
        region = DEFAULT_REGION
    }

    lateinit var testBucket: String

    @BeforeAll
    private fun createResources(): Unit = runBlocking {
        testBucket = S3TestUtils.getTestBucket(client)
    }

    @AfterAll
    private fun cleanup() = runBlocking {
        S3TestUtils.deleteBucketAndAllContents(client, testBucket)
    }

    @Test
    fun testPutObjectFromMemory() = runSuspendTest {
        val contents = """
            Are you suggesting coconuts migrate?
            Not at all, they could be carried.
            What -- a swallow carrying a coconut?
            It could grip it by the husk.
            It's not a question of where he grips it! It's a simple question of weight ratios! A five ounce bird could not carry a 1 pound coconut.
            Well, it doesn't matter. Will you go and tell your master that Arthur from the Court of Camelot is here.
            Listen, in order to maintain air-speed velocity, a swallow needs to beat its wings 43 times times every second, right?
            ...
            It could be carried by an African swallow!
            Oh yeah, an African swallow maybe, but not a European swallow. That's my point.
            Oh, yeah, I agree with that.
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

    @OptIn(ExperimentalTime::class)
    @Test
    fun testPutObjectFromFile() = runSuspendTest {
        val tempFile = RandomTempFile(1024)
        val keyName = "put-obj-from-file.txt"

        // This test fails sporadically (by never completing)
        // see https://github.com/awslabs/aws-sdk-kotlin/issues/282
        withTimeout(Duration.seconds(5)) {
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
}
