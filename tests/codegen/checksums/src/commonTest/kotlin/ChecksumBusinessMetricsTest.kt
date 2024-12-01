/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import aws.sdk.kotlin.test.checksums.TestClient
import aws.sdk.kotlin.test.checksums.httpChecksumOperation
import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.smithy.kotlin.runtime.businessmetrics.SmithyBusinessMetric
import aws.smithy.kotlin.runtime.client.config.HttpChecksumConfigOption
import aws.smithy.kotlin.runtime.httptest.TestEngine
import kotlinx.coroutines.runBlocking
import utils.BusinessMetricsReader
import kotlin.test.Test
import kotlin.test.assertTrue

class ChecksumBusinessMetricsTest {
    @Test
    fun defaultConfigBusinessMetrics(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED,
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }

    @Test
    fun whenSupportedBusinessMetrics(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED,
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
            requestChecksumCalculation = HttpChecksumConfigOption.WHEN_SUPPORTED
            responseChecksumValidation = HttpChecksumConfigOption.WHEN_SUPPORTED
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }

    @Test
    fun whenRequiredBusinessMetrics(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED,
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
            requestChecksumCalculation = HttpChecksumConfigOption.WHEN_REQUIRED
            responseChecksumValidation = HttpChecksumConfigOption.WHEN_REQUIRED
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }

    @Test
    fun crc32(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_CRC32,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Crc32
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }

    @Test
    fun crc32c(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_CRC32C,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Crc32C
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }

    @Test
    fun sha1(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_SHA1,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Sha1
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }

    @Test
    fun sha256(): Unit = runBlocking {
        val businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_SHA256,
            ),
        )

        TestClient {
            httpClient = TestEngine()
            interceptors = mutableListOf(businessMetricsReader)
        }.use { client ->
            client.httpChecksumOperation {
                body = "Hello world".encodeToByteArray()
                checksumAlgorithm = ChecksumAlgorithm.Sha256
            }
        }

        assertTrue(businessMetricsReader.containsExpectedBusinessMetrics)
    }
}
