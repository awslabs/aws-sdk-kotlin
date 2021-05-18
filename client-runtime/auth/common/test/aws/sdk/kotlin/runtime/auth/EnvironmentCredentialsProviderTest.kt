/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.auth.exceptions.AuthenticationException
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnvironmentCredentialsProviderTest {
    private fun provider(vararg vars: Pair<String, String>) = EnvironmentCredentialsProvider((vars.toMap())::get)

    @Test
    fun `it should read from environment variables (incl session token)`() = runSuspendTest {
        val provider = provider(
            EnvironmentCredentialsProvider.accessKeyId to "abc",
            EnvironmentCredentialsProvider.secretAccessKey to "def",
            EnvironmentCredentialsProvider.sessionToken to "ghi",
        )
        assertEquals(provider.getCredentials(), Credentials("abc", "def", "ghi"))
    }

    @Test
    fun `it should read from environment variables (excl session token)`() = runSuspendTest {
        val provider = provider(
            EnvironmentCredentialsProvider.accessKeyId to "abc",
            EnvironmentCredentialsProvider.secretAccessKey to "def",
        )
        assertEquals(provider.getCredentials(), Credentials("abc", "def", null))
    }

    @Test
    fun `it should throw an exception on missing access key`() = runSuspendTest {
        assertFailsWith<AuthenticationException> {
            provider(EnvironmentCredentialsProvider.secretAccessKey to "def").getCredentials()
        }
    }

    @Test
    fun `it should throw an exception on missing secret key`() = runSuspendTest {
        assertFailsWith<AuthenticationException> {
            provider(EnvironmentCredentialsProvider.accessKeyId to "abc").getCredentials()
        }
    }
}
