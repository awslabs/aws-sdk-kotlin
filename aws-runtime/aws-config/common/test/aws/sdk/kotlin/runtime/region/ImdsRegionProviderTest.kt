/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.*
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.httptest.buildTestConnection
import aws.smithy.kotlin.runtime.time.ManualClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime

class ImdsRegionProviderTest {

    @Test
    fun testImdsDisabled() = runSuspendTest {
        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.AwsEc2MetadataDisabled.environmentVariable to "true")
        )

        val provider = ImdsRegionProvider(platformProvider = platform)
        assertNull(provider.getRegion())
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testResolveRegion() = runSuspendTest {

        val connection = buildTestConnection {
            expect(
                tokenRequest("http://169.254.169.254", DEFAULT_TOKEN_TTL_SECONDS),
                tokenResponse(DEFAULT_TOKEN_TTL_SECONDS, "TOKEN_A")
            )
            expect(
                imdsRequest("http://169.254.169.254/latest/meta-data/placement/region", "TOKEN_A"),
                imdsResponse("us-east-2")
            )
        }

        val testClock = ManualClock()

        val client = ImdsClient {
            engine = connection
            clock = testClock
        }

        val provider = ImdsRegionProvider(client = lazyOf(client))
        assertEquals("us-east-2", provider.getRegion())
        connection.assertRequests()

        // test that it's cached, test connection would fail if it tries again
        assertEquals("us-east-2", provider.getRegion())
    }
}
