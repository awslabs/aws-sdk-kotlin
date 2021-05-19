/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = EnvironmentCredentialsProvider((vars.toMap())::get)

    @Test
    fun `it should read from environment variables (incl session token)`() = runSuspendTest {
        val provider = provider(
            EnvironmentCredentialsProvider.ACCESS_KEY_ID to "abc",
            EnvironmentCredentialsProvider.SECRET_ACCESS_KEY to "def",
            EnvironmentCredentialsProvider.SESSION_TOKEN to "ghi",
        )
        assertEquals(provider.getCredentials(), Credentials("abc", "def", "ghi"))
    }

    @Test
    fun `it should read from environment variables (excl session token)`() = runSuspendTest {
        val provider = provider(
            EnvironmentCredentialsProvider.ACCESS_KEY_ID to "abc",
            EnvironmentCredentialsProvider.SECRET_ACCESS_KEY to "def",
        )
        assertEquals(provider.getCredentials(), Credentials("abc", "def", null))
    }

    @Test
    fun `it should throw an exception on missing access key`() = runSuspendTest {
        assertFailsWith<ConfigurationException> {
            provider(EnvironmentCredentialsProvider.SECRET_ACCESS_KEY to "def").getCredentials()
        }
    }

    @Test
    fun `it should throw an exception on missing secret key`() = runSuspendTest {
        assertFailsWith<ConfigurationException> {
            provider(EnvironmentCredentialsProvider.ACCESS_KEY_ID to "abc").getCredentials()
        }
    }
}
