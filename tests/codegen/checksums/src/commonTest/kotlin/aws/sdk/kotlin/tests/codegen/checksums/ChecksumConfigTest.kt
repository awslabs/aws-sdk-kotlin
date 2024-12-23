/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.checksums

import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.sdk.kotlin.test.checksums.model.ValidationMode
import aws.sdk.kotlin.tests.codegen.checksums.utils.HeaderReader
import aws.sdk.kotlin.tests.codegen.checksums.utils.HeaderSetter
import aws.sdk.kotlin.tests.codegen.checksums.utils.runChecksumTest
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestChecksumRequired` param when set to **true**.
 */
class RequestChecksumRequired {
    @Test
    fun requestChecksumCalculationWhenSupported() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        ),
        requestChecksumCalculationValue = RequestHttpChecksumConfig.WHEN_SUPPORTED,
    )

    @Test
    fun requestChecksumCalculationWhenRequired() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        ),
        requestChecksumCalculationValue = RequestHttpChecksumConfig.WHEN_REQUIRED,
    )
}

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestChecksumRequired` param when set to **false**.
 */
class RequestChecksumNotRequired {
    @Test
    fun requestChecksumCalculationWhenSupported() = runChecksumTest(
        requestChecksumRequired = false,
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
        ),
        requestChecksumCalculationValue = RequestHttpChecksumConfig.WHEN_SUPPORTED,
    )

    @Test
    fun requestChecksumCalculationWhenRequired() = runChecksumTest(
        requestChecksumRequired = false,
        headerReader = HeaderReader(
            forbiddenHeaders = mapOf("x-amz-checksum-crc32" to null),
        ),
        requestChecksumCalculationValue = RequestHttpChecksumConfig.WHEN_REQUIRED,
    )
}

/**
 * Tests user selected checksum **algorithm**
 */
class UserSelectedChecksumAlgorithm {
    @Test
    fun userSelectedChecksumAlgorithmIsUsed() = runChecksumTest(
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-sha256" to null),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha256,
    )
}

/**
 * Tests user provided checksum **calculation**
 */
class UserProvidedChecksumHeader {
    @Test
    fun userProvidedChecksumIsUsed() = runChecksumTest(
        headerSetter = HeaderSetter(
            mapOf("x-amz-checksum-crc64nvme" to "foo"),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha256,
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc64nvme" to "foo"),
            forbiddenHeaders = mapOf(
                "x-amz-checksum-sha256" to "foo", // Should be ignored since header checksum has priority
                "x-amz-checksum-crc32" to "foo", // Default checksum shouldn't be used
            ),
        ),
    )

    @Test
    fun newChecksumAlgorithmIsUsed() = runChecksumTest(
        headerSetter = HeaderSetter(
            mapOf("x-amz-checksum-some-future-algorithm" to "foo"),
        ),
        checksumAlgorithmValue = ChecksumAlgorithm.Sha256,
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-some-future-algorithm" to "foo"),
            forbiddenHeaders = mapOf(
                "x-amz-checksum-sha256" to "foo",
                "x-amz-checksum-crc32" to "foo",
            ),
        ),
    )

    @Test
    fun md5IsNotUsed() = runChecksumTest(
        headerSetter = HeaderSetter(
            mapOf("x-amz-checksum-md5" to "foo"),
        ),
        headerReader = HeaderReader(
            expectedHeaders = mapOf("x-amz-checksum-crc32" to null),
            forbiddenHeaders = mapOf("x-amz-checksum-md5" to "foo"), // MD5 is not supported for flexible checksums
        ),
    )
}

/**
 * Tests the `aws.protocols#httpChecksum` trait's `requestValidationModeMember`.
 */
class ResponseChecksumValidation {
    private val incorrectChecksumValue = "Kaboom!"

    @Test
    fun whenRequiredAndNotEnabled() = runChecksumTest(
        responseChecksumValidationValue = ResponseHttpChecksumConfig.WHEN_REQUIRED,
        responseChecksumHeader = "x-amz-checksum-crc32",
        responseChecksumValue = incorrectChecksumValue,
    )

    @Test
    fun whenSupportedAndNotEnabled() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumValidationValue = ResponseHttpChecksumConfig.WHEN_SUPPORTED,
                responseChecksumHeader = "x-amz-checksum-crc32",
                responseChecksumValue = incorrectChecksumValue,
            )
        }
    }

    @Test
    fun whenRequiredAndEnabled() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumValidationValue = ResponseHttpChecksumConfig.WHEN_REQUIRED,
                responseChecksumHeader = "x-amz-checksum-crc32",
                responseChecksumValue = incorrectChecksumValue,
                validationModeValue = ValidationMode.Enabled,
            )
        }
    }

    @Test
    fun whenSupportedAndEnabled() {
        assertFailsWith<ChecksumMismatchException> {
            runChecksumTest(
                responseChecksumValidationValue = ResponseHttpChecksumConfig.WHEN_SUPPORTED,
                responseChecksumHeader = "x-amz-checksum-crc32",
                responseChecksumValue = incorrectChecksumValue,
                validationModeValue = ValidationMode.Enabled,
            )
        }
    }
}
