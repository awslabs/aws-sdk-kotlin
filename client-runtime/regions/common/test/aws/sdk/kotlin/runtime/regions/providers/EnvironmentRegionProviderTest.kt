/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvironmentRegionProviderTest {

    @Test
    fun noRegion() = runSuspendTest {
        val environ = mapOf<String, String>()
        val provider = EnvironmentRegionProvider { environ[it] }
        assertNull(provider.getRegion())
    }

    @Test
    fun prioritizesAwsRegion() = runSuspendTest {
        val environ = mapOf(
            "AWS_REGION" to "us-east-1",
            "AWS_DEFAULT_REGION" to "us-east-2"
        )

        val provider = EnvironmentRegionProvider { environ[it] }

        assertEquals("us-east-1", provider.getRegion())
    }

    @Test
    fun fallbackToDefaultRegion() = runSuspendTest {
        val environ = mapOf(
            "AWS_DEFAULT_REGION" to "us-east-2"
        )

        val provider = EnvironmentRegionProvider { environ[it] }

        assertEquals("us-east-2", provider.getRegion())
    }
}
