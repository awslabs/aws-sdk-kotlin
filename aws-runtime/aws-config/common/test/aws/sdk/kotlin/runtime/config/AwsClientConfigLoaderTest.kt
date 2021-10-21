/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
