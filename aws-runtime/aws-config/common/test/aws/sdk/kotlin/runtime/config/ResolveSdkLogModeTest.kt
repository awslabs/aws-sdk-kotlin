/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.client.LogMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ResolveLogModeTest {

    @Test
    fun itResolvesLogModeFromEnvironmentVariables() = runTest {
        val expectedLogMode = LogMode.LogRequest

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.LogMode.environmentVariable to expectedLogMode.toString()),
        )

        assertEquals(expectedLogMode, resolveLogMode(platform))
    }

    @Test
    fun itResolvesLogModeFromSystemProperties() = runTest {
        val expectedLogMode = LogMode.LogRequestWithBody + LogMode.LogResponseWithBody
        val platform = TestPlatformProvider(
            props = mapOf(AwsSdkSetting.LogMode.jvmProperty to "LogRequestWithBody|LogResponseWithBody"),
        )

        assertEquals(expectedLogMode, resolveLogMode(platform))
    }

    @Test
    fun itThrowsOnInvalidLogModeFromEnvironmentVariable() = runTest {
        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.LogMode.environmentVariable to "InvalidLogMode"),
        )
        assertFailsWith<ClientException> { resolveLogMode(platform) }
    }

    @Test
    fun itThrowsOnInvalidLogModeFromSystemProperty() = runTest {
        val platform = TestPlatformProvider(
            props = mapOf(AwsSdkSetting.LogMode.jvmProperty to "InvalidLogMode"),
        )
        assertFailsWith<ClientException> { resolveLogMode(platform) }
    }

    @Test
    fun itResolvesNonLowercaseLogModesFromEnvironmentVariables() = runTest {
        val expectedLogMode = LogMode.LogRequest
        val nonLowercaseLogMode = "lOgReQUEST"

        val platform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.LogMode.environmentVariable to nonLowercaseLogMode,
            ),
        )

        assertEquals(expectedLogMode, resolveLogMode(platform))
    }

    @Test
    fun itResolvesNonLowercaseLogModesFromSystemProperty() = runTest {
        val expectedLogMode = LogMode.allModes().reduce { acc, logMode -> acc + logMode }
        val nonLowercaseLogMode = "LOGREQUest|logReSponSe|logREQUESTwithBODY|LoGrEsPoNsEWitHBoDY"

        val platform = TestPlatformProvider(
            props = mapOf(
                AwsSdkSetting.LogMode.jvmProperty to nonLowercaseLogMode,
            ),
        )

        assertEquals(expectedLogMode, resolveLogMode(platform))
    }

    @Test
    fun itResolvesWithEnvironmentVariablePriority() = runTest {
        // set the system property and environment variable. resolution should prioritize system property
        val platform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.LogMode.environmentVariable to "invalid-sdk-log-mode-should-be-ignored",
            ),
            props = mapOf(
                AwsSdkSetting.LogMode.jvmProperty to "LogRequest",
            ),
        )

        assertEquals(LogMode.LogRequest, resolveLogMode(platform))
    }

    @Test
    fun itUsesDefaultLogModeWhenNoneAreConfigured() = runTest {
        val expectedLogMode = LogMode.Default

        val platform = TestPlatformProvider() // no environment variables / system properties / profile

        assertEquals(expectedLogMode, resolveLogMode(platform))
    }
}
