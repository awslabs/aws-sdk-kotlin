/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StaticCredentialsProviderTest {
    @Test
    fun testStaticProvider() = runTest {
        val expected = Credentials("access_key_id", "secret_access_key", "session_token")
        val provider = StaticCredentialsProvider(expected)
        assertEquals(expected, provider.getCredentials())

        val provider2 = StaticCredentialsProvider {
            accessKeyId = expected.accessKeyId
            secretAccessKey = expected.secretAccessKey
            sessionToken = expected.sessionToken
        }
        assertEquals(expected, provider2.getCredentials())
    }
}
