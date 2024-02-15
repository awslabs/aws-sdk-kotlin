/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

public class S3ExpressCredentialsCacheTest {
    @Test
    fun testCacheKeyEquality() = runTest {
        val bucket = "bucket"
        val testCredentials = Credentials("accessKeyId", "secretAccessKey", "sessionToken")

        // Different keys with the same bucket and credentials should be considered equal
        val key1 = S3ExpressCredentialsCacheKey(bucket, testCredentials)
        val key2 = S3ExpressCredentialsCacheKey(bucket, testCredentials)

        assertEquals(key1, key2)
    }
}
