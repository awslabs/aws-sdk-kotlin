/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmSystemPropRegionProviderTest {

    @Test
    fun testGetRegion() = runSuspendTest {
        val provider = JvmSystemPropRegionProvider(TestPlatformProvider())

        assertNull(provider.getRegion())

        val provider2 = JvmSystemPropRegionProvider(
            TestPlatformProvider(
                props = mapOf("aws.region" to "us-east-1")
            )
        )
        assertEquals("us-east-1", provider2.getRegion())
    }
}
