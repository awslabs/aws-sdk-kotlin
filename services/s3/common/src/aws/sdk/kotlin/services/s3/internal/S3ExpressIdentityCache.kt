/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.collections.LruCache
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.ExpiringValue

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100

public class S3ExpressCredentialsCache(
    private val clock: Clock = Clock.System,
) {
    private val lru = LruCache<S3ExpressCredentialsCacheKey, ExpiringValue<Credentials>>(DEFAULT_S3_EXPRESS_CACHE_SIZE)

    public suspend fun get(key: S3ExpressCredentialsCacheKey): Credentials? = (
        lru.get(key)
            ?.takeIf { it.expiresAt > clock.now() }
        )?.value

    public suspend fun put(key: S3ExpressCredentialsCacheKey, value: ExpiringValue<Credentials>): Unit = lru.put(key, value)
}

public class S3ExpressCredentialsCacheKey(
    public val bucket: String,
    public val client: SdkClient,
    public val credentials: Credentials,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is S3ExpressCredentialsCacheKey) return false
        if (bucket != other.bucket) return false
        if (client != other.client) return false
        if (credentials != other.credentials) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bucket.hashCode() ?: 0
        result = 31 * result + (client.hashCode() ?: 0)
        result = 31 * result + (credentials.hashCode() ?: 0)
        return result
    }
}
