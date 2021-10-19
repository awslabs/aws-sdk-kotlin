/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.ClientException
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class CredentialsProviderChainTest {
    @Test
    fun testNoProviders() {
        assertFails("at least one provider") {
            CredentialsProviderChain()
        }
    }
    data class TestProvider(val credentials: Credentials? = null) : CredentialsProvider {
        override suspend fun getCredentials(): Credentials = credentials ?: throw IllegalStateException("no credentials available")
    }

    @Test
    fun testChain() = runSuspendTest {
        val chain = CredentialsProviderChain(
            TestProvider(null),
            TestProvider(Credentials("akid1", "secret1")),
            TestProvider(Credentials("akid2", "secret2"))
        )

        assertEquals(Credentials("akid1", "secret1"), chain.getCredentials())
    }

    @Test
    fun testChainNoCredentials(): Unit = runSuspendTest {
        val chain = CredentialsProviderChain(
            TestProvider(null),
            TestProvider(null),
        )

        assertFailsWith<ClientException> {
            chain.getCredentials()
        }.message.shouldContain("No credentials could be loaded from the chain: CredentialsProviderChain -> TestProvider -> TestProvider")
    }
}
