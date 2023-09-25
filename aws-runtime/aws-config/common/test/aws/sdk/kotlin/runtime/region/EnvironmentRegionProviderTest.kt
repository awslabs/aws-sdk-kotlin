/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.smithy.kotlin.runtime.util.EnvironmentProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EnvironmentRegionProviderTest {
    fun Map<String, String>.asEnvironmentProvider() = object : EnvironmentProvider {
        override fun getAllEnvVars(): Map<String, String> = this@asEnvironmentProvider
        override fun getenv(key: String): String? = this@asEnvironmentProvider[key]
    }

    @Test
    fun noRegion() = runTest {
        val environ = mapOf<String, String>()
        val provider = EnvironmentRegionProvider(environ.asEnvironmentProvider())
        assertNull(provider.getRegion())
    }

    @Test
    fun providesRegion() = runTest {
        val environ = mapOf(
            "AWS_REGION" to "us-east-1",
        )

        val provider = EnvironmentRegionProvider(environ.asEnvironmentProvider())

        assertEquals("us-east-1", provider.getRegion())
    }
}
