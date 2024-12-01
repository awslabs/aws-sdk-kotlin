/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.test.checksums.*
import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.runBlocking
import utils.HeaderReader
import kotlin.test.Test
import kotlin.test.assertTrue

class ChecksumRequestTest {
    @Test
    fun crc32(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "CRC32",
                "x-amz-checksum-crc32" to "i9aeUg==",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Crc32
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }

    @Test
    fun crc32c(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "CRC32C",
                "x-amz-checksum-crc32c" to "crUfeA==",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Crc32C
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }

    @Test
    fun sha1(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "SHA1",
                "x-amz-checksum-sha1" to "e1AsOh9IyGCa4hLN+2Od7jlnP14=",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Sha1
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }

    @Test
    fun sha256(): Unit = runBlocking {
        val headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "SHA256",
                "x-amz-checksum-sha256" to "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=",
            ),
        )

        TestClient {
            interceptors = mutableListOf(headerReader)
            httpClient = TestEngine()
            credentialsProvider = StaticCredentialsProvider(
                Credentials("accessKeyID", "secretAccessKey"),
            )
            region = "us-east-1"
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Sha256
            }
        }

        assertTrue(
            headerReader.containsExpectedHeaders,
        )
    }
}
