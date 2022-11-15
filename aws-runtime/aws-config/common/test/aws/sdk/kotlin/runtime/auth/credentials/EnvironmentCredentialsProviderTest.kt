/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = EnvironmentCredentialsProvider((vars.toMap())::get)

    @Test
    fun `it should read from environment variables (incl session token)`() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.environmentVariable to "abc",
            AwsSdkSetting.AwsSecretAccessKey.environmentVariable to "def",
            AwsSdkSetting.AwsSessionToken.environmentVariable to "ghi",
        )
        val actual = provider.getCredentials()
        assertEquals(Credentials("abc", "def", "ghi", providerName = "Environment"), actual)
    }

    @Test
    fun `it should read from environment variables (excl session token)`() = runTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.environmentVariable to "abc",
            AwsSdkSetting.AwsSecretAccessKey.environmentVariable to "def",
        )
        val actual = provider.getCredentials()
        assertEquals(Credentials("abc", "def", null, providerName = "Environment"), actual)
    }

    @Test
    fun `it should throw an exception on missing access key`() = runTest {
        val provider = provider(AwsSdkSetting.AwsSecretAccessKey.environmentVariable to "def")
        assertFailsWith<ProviderConfigurationException> {
            provider.getCredentials()
        }.message.shouldContain("Missing value for environment variable `AWS_ACCESS_KEY_ID`")
    }

    @Test
    fun `it should throw an exception on missing secret key`() = runTest {
        val provider = provider(AwsSdkSetting.AwsAccessKeyId.environmentVariable to "abc")
        assertFailsWith<ProviderConfigurationException> {
            provider.getCredentials()
        }.message.shouldContain("Missing value for environment variable `AWS_SECRET_ACCESS_KEY`")
    }
}
