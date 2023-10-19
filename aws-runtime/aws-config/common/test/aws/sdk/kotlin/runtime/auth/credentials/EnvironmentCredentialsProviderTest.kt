/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = EnvironmentCredentialsProvider((vars.toMap())::get)

    @Test
    fun testReadFromEnvironmentIncludingSessionToken() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
            AwsSdkSetting.AwsSecretAccessKey.envVar to "def",
            AwsSdkSetting.AwsSessionToken.envVar to "ghi",
        )
        assertEquals(provider.resolve(), Credentials("abc", "def", "ghi", providerName = "Environment"))
    }

    @Test
    fun testReadFromEnvironmentExcludingSessionToken() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
            AwsSdkSetting.AwsSecretAccessKey.envVar to "def",
        )
        assertEquals(provider.resolve(), Credentials("abc", "def", null, providerName = "Environment"))
    }

    @Test
    fun testThrowsWhenMissingAccessKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsSecretAccessKey.envVar to "def").resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_ACCESS_KEY_ID`")
    }

    @Test
    fun testThrowsWhenMissingSecretKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsAccessKeyId.envVar to "abc").resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_SECRET_ACCESS_KEY`")
    }

    @Test
    fun testIgnoresEmptyAccessKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(
                AwsSdkSetting.AwsAccessKeyId.envVar to "",
                AwsSdkSetting.AwsSecretAccessKey.envVar to "abc",
            ).resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_ACCESS_KEY_ID`")
    }

    @Test
    fun testIgnoresEmptySecretKey() = runTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(
                AwsSdkSetting.AwsAccessKeyId.envVar to "abc",
                AwsSdkSetting.AwsSecretAccessKey.envVar to "",
            ).resolve()
        }.message.shouldContain("Missing value for environment variable `AWS_SECRET_ACCESS_KEY`")
    }
}
