/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.services.s3

import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Validate the way service clients can be constructed.
 *
 * These are written against S3 but apply generically to any client.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateClientTest {

    @Test
    fun testMissingRegion() {
        assertFailsWith<IllegalArgumentException> {
            S3Client { }
        }.message.shouldContain("region is a required configuration property")
    }

    @Test
    fun testDefaults() {
        S3Client { region = "us-east-2" }.use { }
    }

    @Test
    fun testFromEnvironmentWithOverrides() = runTest {
        S3Client.fromEnvironment { region = "us-east-2" }.use { }
    }
}
