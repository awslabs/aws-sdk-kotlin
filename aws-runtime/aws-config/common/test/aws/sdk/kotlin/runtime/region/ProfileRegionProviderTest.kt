/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.profile.AwsConfigValue
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileRegionProviderTest {
    @Test
    fun testSuccessDefaultProfile() = runTest {
        val profile = AwsProfile(
            "default",
            mapOf(
                "region" to AwsConfigValue.String("us-east-2"),
            ),
        )

        val lazyProfile = asyncLazy { profile }
        val provider = ProfileRegionProvider(lazyProfile)
        assertEquals("us-east-2", provider.getRegion())
    }

    @Test
    fun testNoRegion() = runTest {
        val profile = AwsProfile(
            "default",
            mapOf(
                "not-region" to AwsConfigValue.String("us-east-2"),
            ),
        )

        val lazyProfile = asyncLazy { profile }
        val provider = ProfileRegionProvider(lazyProfile)
        assertEquals(null, provider.getRegion())
    }
}
