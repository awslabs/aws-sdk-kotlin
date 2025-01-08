/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.checksums

import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.sdk.kotlin.tests.codegen.checksums.utils.BusinessMetricsReader
import aws.sdk.kotlin.tests.codegen.checksums.utils.runChecksumTest
import aws.smithy.kotlin.runtime.businessmetrics.SmithyBusinessMetric
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import kotlin.test.Test

class ChecksumBusinessMetricsTest {
    @Test
    fun defaultConfigBusinessMetrics() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED,
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED,
            ),
        ),
    )

    @Test
    fun whenSupportedBusinessMetrics() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_WHEN_SUPPORTED,
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_RES_WHEN_SUPPORTED,
            ),
        ),
        requestChecksumCalculationValue = RequestHttpChecksumConfig.WHEN_SUPPORTED,
        responseChecksumValidationValue = ResponseHttpChecksumConfig.WHEN_SUPPORTED,
    )

    @Test
    fun whenRequiredBusinessMetrics() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_WHEN_REQUIRED,
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_RES_WHEN_REQUIRED,
            ),
        ),
        requestChecksumCalculationValue = RequestHttpChecksumConfig.WHEN_REQUIRED,
        responseChecksumValidationValue = ResponseHttpChecksumConfig.WHEN_REQUIRED,
    )

    @Test
    fun crc32() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_CRC32,
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Crc32,
    )

    @Test
    fun crc32c() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_CRC32C,
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Crc32C,
    )

    @Test
    fun sha1() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_SHA1,
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha1,
    )

    @Test
    fun sha256() = runChecksumTest(
        businessMetricsReader = BusinessMetricsReader(
            expectedBusinessMetrics = setOf(
                SmithyBusinessMetric.FLEXIBLE_CHECKSUMS_REQ_SHA256,
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha256,
    )
}
