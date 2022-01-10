/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.io.Closeable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AwsClientConfigLoaderTest {
    @Test
    fun testExplicit(): Unit = runSuspendTest {
        val provider = TestPlatformProvider()
        val actual = loadAwsClientConfig(provider) {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "AKID"
                secretAccessKey = "secret"
            }
        }
        assertEquals("us-east-2", actual.region)
        assertEquals("AKID", actual.credentialsProvider.getCredentials().accessKeyId)
    }

    @Test
    fun testDefaults(): Unit = runSuspendTest {
        val provider = TestPlatformProvider(env = mapOf("AWS_REGION" to "us-east-2"))
        val actual = loadAwsClientConfig(provider) {}
        assertEquals("us-east-2", actual.region)
        assertIs<DefaultChainCredentialsProvider>(actual.credentialsProvider)
    }

    @Test
    fun testClose(): Unit = runSuspendTest {
        val provider = TestPlatformProvider()
        val borrowedProvider = object : CredentialsProvider, Closeable {
            override suspend fun getCredentials(): Credentials { error("not needed for test") }
            override fun close() { error("cred provider should be borrowed") }
        }

        val actual = loadAwsClientConfig(provider) {
            region = "us-east-2"
            credentialsProvider = borrowedProvider
        }
        // fails if borrowed.close() is called
        actual.close()

        val defaultProvider = loadAwsClientConfig(provider) {
            region = "us-east-2"
        }
        defaultProvider.close()
    }
}
