/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = EnvironmentCredentialsProvider((vars.toMap())::get)

    @Test
    fun `it should read from environment variables (incl session token)`() = runSuspendTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.environmentVariable to "abc",
            AwsSdkSetting.AwsSecretAccessKey.environmentVariable to "def",
            AwsSdkSetting.AwsSessionToken.environmentVariable to "ghi",
        )
        assertEquals(provider.getCredentials(), Credentials("abc", "def", "ghi"))
    }

    @Test
    fun `it should read from environment variables (excl session token)`() = runSuspendTest {
        val provider = provider(
            AwsSdkSetting.AwsAccessKeyId.environmentVariable to "abc",
            AwsSdkSetting.AwsSecretAccessKey.environmentVariable to "def",
        )
        assertEquals(provider.getCredentials(), Credentials("abc", "def", null))
    }

    @Test
    fun `it should throw an exception on missing access key`(): Unit = runSuspendTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsSecretAccessKey.environmentVariable to "def").getCredentials()
        }.message.shouldContain("Missing value for environment variable `AWS_ACCESS_KEY_ID`")
    }

    @Test
    fun `it should throw an exception on missing secret key`(): Unit = runSuspendTest {
        assertFailsWith<ProviderConfigurationException> {
            provider(AwsSdkSetting.AwsAccessKeyId.environmentVariable to "abc").getCredentials()
        }.message.shouldContain("Missing value for environment variable `AWS_SECRET_ACCESS_KEY`")
    }
}
