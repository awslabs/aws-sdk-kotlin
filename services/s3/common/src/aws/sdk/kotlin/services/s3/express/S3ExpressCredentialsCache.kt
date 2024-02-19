/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.sdk.kotlin.services.s3.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.LruCache
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.telemetry.TelemetryProviderContext
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.until
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100

internal class S3ExpressCredentialsCache {
    private val lru = LruCache<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>(DEFAULT_S3_EXPRESS_CACHE_SIZE)

    suspend fun get(key: S3ExpressCredentialsCacheKey): ExpiringValue<Credentials>? = lru.get(key)?.expiringCredentials

    suspend fun put(key: S3ExpressCredentialsCacheKey, value: ExpiringValue<Credentials>, usedSinceLastRefresh: Boolean = true) =
        lru.put(key, S3ExpressCredentialsCacheValue(value, usedSinceLastRefresh))

    suspend fun remove(key: S3ExpressCredentialsCacheKey) : ExpiringValue<Credentials>? =
        lru.remove(key)?.expiringCredentials

    public val size: Int
        get() = lru.size

    public val entries: Set<Map.Entry<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>>
        get() = lru.entries

//    suspend fun get(key: S3ExpressCredentialsCacheKey): Credentials = lru
//        .get(key)
//        ?.takeIf { !it.expiringCredentials.isExpired }
//        ?.let {
//            it.usedSinceLastRefresh = true
//            it.expiringCredentials.value
//        }
//        ?: (createSessionCredentials(key.bucket).also { put(key, it) }).value


}

internal data class S3ExpressCredentialsCacheKey(
    val bucket: String,
    val credentials: Credentials,
)

internal data class S3ExpressCredentialsCacheValue(
    val expiringCredentials: ExpiringValue<Credentials>,
    var usedSinceLastRefresh: Boolean,
)

/**
 * A value with an expiration [TimeMark]
 */
internal data class ExpiringValue<T> (val value: T, val expiresAt: TimeMark)

internal val ExpiringValue<Credentials>.isExpired: Boolean get() = (expiresAt + REFRESH_BUFFER).hasPassedNow()