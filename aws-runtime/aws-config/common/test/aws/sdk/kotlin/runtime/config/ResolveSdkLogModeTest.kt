/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.smithy.kotlin.runtime.client.SdkLogMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ResolveSdkLogModeTest {

    @Test
    fun itResolvesSdkLogModeFromEnvironmentVariables() = runTest {
        val expectedSdkLogMode = SdkLogMode.LogRequest

        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.SdkLogMode.environmentVariable to expectedSdkLogMode.toString()),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itResolvesSdkLogModeFromSystemProperties() = runTest {
        val expectedSdkLogMode = SdkLogMode.LogRequestWithBody + SdkLogMode.LogResponseWithBody
        val platform = TestPlatformProvider(
            props = mapOf(AwsSdkSetting.SdkLogMode.jvmProperty to "LogRequestWithBody|LogResponseWithBody"),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itResolvesSdkLogModeFromProfile() = runTest {
        val expectedSdkLogMode = SdkLogMode.LogResponse

        val platform = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                sdk_log_mode=$expectedSdkLogMode
                """.trimIndent(),
            ),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itThrowsOnInvalidSdkLogModeFromEnvironmentVariable() = runTest {
        val platform = TestPlatformProvider(
            env = mapOf(AwsSdkSetting.SdkLogMode.environmentVariable to "InvalidSdkLogMode"),
        )
        assertFailsWith<ConfigurationException> { resolveSdkLogMode(platform) }
    }

    @Test
    fun itThrowsOnInvalidSdkLogModeFromSystemProperty() = runTest {
        val platform = TestPlatformProvider(
            props = mapOf(AwsSdkSetting.SdkLogMode.jvmProperty to "InvalidSdkLogMode"),
        )
        assertFailsWith<ConfigurationException> { resolveSdkLogMode(platform) }
    }

    @Test
    fun itThrowsOnInvalidSdkLogModeFromProfile() = runTest {
        val platform = TestPlatformProvider(
            env = mapOf("AWS_CONFIG_FILE" to "config"),
            fs = mapOf(
                "config" to """
                [default]
                sdk_log_mode=InvalidSdkLogMode
                """.trimIndent(),
            ),
        )
        assertFailsWith<ConfigurationException> { resolveSdkLogMode(platform) }
    }

    @Test
    fun itResolvesNonLowercaseSdkLogModesFromEnvironmentVariables() = runTest {
        val expectedSdkLogMode = SdkLogMode.LogRequest
        val nonLowercaseSdkLogMode = "lOgReQUEST"

        val platform = TestPlatformProvider(
            env = mapOf(
                AwsSdkSetting.SdkLogMode.environmentVariable to nonLowercaseSdkLogMode,
            ),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itResolvesNonLowercaseSdkLogModesFromSystemProperty() = runTest {
        val expectedSdkLogMode = SdkLogMode.allModes().reduce { acc, sdkLogMode -> acc + sdkLogMode }
        val nonLowercaseSdkLogMode = "LOGREQUest|logReSponSe|logREQUESTwithBODY|LoGrEsPoNsEWitHBoDY"

        val platform = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to """
                [default]
                sdk_log_mode=$nonLowercaseSdkLogMode
                """.trimIndent(),
            ),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itResolvesNonLowercaseSdkLogModesFromProfile() = runTest {
        val expectedSdkLogMode = SdkLogMode.LogRequest + SdkLogMode.LogResponse
        val nonLowercaseSdkLogMode = "logREQUEST|logRESPONSe"

        val platform = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
            ),
            fs = mapOf(
                "config" to """
                [default]
                sdk_log_mode=$nonLowercaseSdkLogMode
                """.trimIndent(),
            ),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itResolvesWithEnvironmentVariablePriority() = runTest {
        // set the profile and environment variable. resolution should prioritize environment variables
        val expectedSdkLogMode = SdkLogMode.LogRequest

        val platform = TestPlatformProvider(
            env = mapOf(
                "AWS_CONFIG_FILE" to "config",
                AwsSdkSetting.SdkLogMode.environmentVariable to expectedSdkLogMode.toString(),
            ),
            fs = mapOf(
                "config" to """
                [default]
                sdk_log_mode=invalid-sdk-log-mode-should-be-ignored
                """.trimIndent(),
            ),
        )

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }

    @Test
    fun itUsesDefaultSdkLogModeWhenNoneAreConfigured() = runTest {
        val expectedSdkLogMode = SdkLogMode.Default

        val platform = TestPlatformProvider() // no environment variables / system properties / profile

        assertEquals(expectedSdkLogMode, resolveSdkLogMode(platform))
    }
}
