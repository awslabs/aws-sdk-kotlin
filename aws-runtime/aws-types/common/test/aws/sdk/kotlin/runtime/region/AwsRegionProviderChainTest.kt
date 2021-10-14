/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AwsRegionProviderChainTest {
    @Test
    fun testNoProviders() {
        assertFails("at least one provider") {
            RegionProviderChain()
        }
    }
    data class TestProvider(val region: String? = null) : RegionProvider {
        override suspend fun getRegion(): String? = region
    }

    @Test
    fun testChain() = runSuspendTest {
        val chain = RegionProviderChain(
            TestProvider(null),
            TestProvider("us-east-1"),
            TestProvider("us-east-2")
        )

        assertEquals("us-east-1", chain.getRegion())
    }
}
