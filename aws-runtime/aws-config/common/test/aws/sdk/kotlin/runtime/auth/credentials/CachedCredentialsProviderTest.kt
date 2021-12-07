/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import kotlin.test.Test

class CachedCredentialsProviderTest {
    @Test
    fun testLoadFirstCall() {
        TODO()
    }

    @Test
    fun testReloadExpiredCredentials() {
        val source = StaticCredentialsProvider {}
        val provider = CachedCredentialsProvider(source)
        TODO()
    }
    @Test
    fun testLoadFailed() {
        TODO("add test for source provider failing to load credentials")
    }

    @Test
    fun testContention() {
        TODO("add test for multiple coroutines attempting to fetch credentials, load should only be called once for all of them")
    }
}
