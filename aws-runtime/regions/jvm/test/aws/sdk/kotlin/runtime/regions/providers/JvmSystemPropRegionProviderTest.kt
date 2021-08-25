/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

import aws.sdk.kotlin.runtime.AwsSdkSetting
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmSystemPropRegionProviderTest {

    @Test
    fun testGetRegion() = runSuspendTest {
        val provider = JvmSystemPropRegionProvider()

        assertNull(provider.getRegion())

        System.setProperty(AwsSdkSetting.AwsRegion.jvmProperty, "us-east-1")
        assertEquals("us-east-1", provider.getRegion())
    }
}
