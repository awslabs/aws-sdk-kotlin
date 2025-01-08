/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.checksums

import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.sdk.kotlin.tests.codegen.checksums.utils.HeaderReader
import aws.sdk.kotlin.tests.codegen.checksums.utils.runChecksumTest
import kotlin.test.Test

/**
 * Tests headers match the configured checksum algorithm
 */
class ChecksumRequestTest {
    @Test
    fun crc32() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "CRC32",
                "x-amz-checksum-crc32" to "i9aeUg==",
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Crc32,
    )

    @Test
    fun crc32c() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "CRC32C",
                "x-amz-checksum-crc32c" to "crUfeA==",
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Crc32C,
    )

    @Test
    fun sha1() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "SHA1",
                "x-amz-checksum-sha1" to "e1AsOh9IyGCa4hLN+2Od7jlnP14=",
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha1,
    )

    @Test
    fun sha256() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf(
                "x-amz-request-algorithm" to "SHA256",
                "x-amz-checksum-sha256" to "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=",
            ),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha256,
    )
}
