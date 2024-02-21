/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.LruCache
import kotlin.time.TimeMark

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100

internal typealias S3ExpressCredentialsCache = LruCache<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>
internal fun S3ExpressCredentialsCache() = S3ExpressCredentialsCache(DEFAULT_S3_EXPRESS_CACHE_SIZE)

internal data class S3ExpressCredentialsCacheKey(
    val bucket: String,
    val credentials: Credentials,
)

internal data class S3ExpressCredentialsCacheValue(
    val expiringCredentials: ExpiringValue<Credentials>,
    var usedSinceLastRefresh: Boolean = false,
)

/**
 * A value with an expiration [TimeMark]
 */
internal data class ExpiringValue<T>(val value: T, val expiresAt: TimeMark)

internal val ExpiringValue<Credentials>.isExpired: Boolean get() = (expiresAt - REFRESH_BUFFER).hasPassedNow()

internal typealias S3ExpressCredentialsCacheEntry = Map.Entry<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>
