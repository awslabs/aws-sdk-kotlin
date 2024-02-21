/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

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

    @Test
    fun testCacheOperations() = runTest {
        val cache = S3ExpressCredentialsCache()

        val bucket = "bucket"
        val bootstrapCredentials = Credentials("accessKeyId", "secretAccessKey", "sessionToken")
        val key = S3ExpressCredentialsCacheKey(bucket, bootstrapCredentials)

        val sessionCredentials = Credentials("superFastAccessKey", "superSecretSecretKey", "s3SessionToken")
        val expiringSessionCredentials = ExpiringValue(sessionCredentials, TestTimeSource().markNow())
        val value = S3ExpressCredentialsCacheValue(expiringSessionCredentials)

        cache.put(key, value) // put
        assertEquals(expiringSessionCredentials, cache.get(key)?.expiringCredentials) // get
        assertEquals(1, cache.size) // size
        assertContains(cache.entries.map { it.key }, key) // entries
        assertContains(cache.entries.map { it.value }, value) // entries

        cache.remove(key)
        assertEquals(0, cache.size)
        assertNull(cache.get(key))
    }

    @Test
    fun testIsExpired() = runTest {
        val timeSource = TestTimeSource()

        val sessionCredentials = Credentials("superFastAccessKey", "superSecretSecretKey", "s3SessionToken")

        val expiringSessionCredentials = ExpiringValue(sessionCredentials, timeSource.markNow() + 5.minutes)
        assertFalse(expiringSessionCredentials.isExpired)

        timeSource += 5.minutes + 1.seconds // advance just past the expiration time
        assertTrue(expiringSessionCredentials.isExpired)
    }

    @Test
    fun testIsWithin() = runTest {
        val timeSource = TestTimeSource()

        val sessionCredentials = Credentials("superFastAccessKey", "superSecretSecretKey", "s3SessionToken")

        val expiringSessionCredentials = ExpiringValue(sessionCredentials, timeSource.markNow() + 1.minutes)
        assertFalse(expiringSessionCredentials.isExpiringWithin(30.seconds))

        timeSource += 31.seconds
        assertTrue(expiringSessionCredentials.isExpiringWithin(30.seconds))
    }
}
