/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.LruCache
import aws.smithy.kotlin.runtime.util.SingleFlightGroup
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.TimeMark

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100

internal typealias S3ExpressCredentialsCache = LruCache<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>
internal fun S3ExpressCredentialsCache() = S3ExpressCredentialsCache(DEFAULT_S3_EXPRESS_CACHE_SIZE)

internal data class S3ExpressCredentialsCacheKey(
    /**
     * The directory bucket requests are being made to
     */
    val bucket: String,
    /**
     * The base credentials used to resolve session credentials
     */
    val baseCredentials: Credentials,
)

internal data class S3ExpressCredentialsCacheValue(
    /**
     * The expiring session [Credentials]
     */
    val expiringCredentials: ExpiringValue<Credentials>,
    /**
     * A [SingleFlightGroup] used to de-duplicate asynchronous refresh attempts
     */
    val sfg: SingleFlightGroup<ExpiringValue<Credentials>> = SingleFlightGroup(),
)

/**
 * A value with an expiration [TimeMark]
 */
internal data class ExpiringValue<T>(val value: T, val expiresAt: ComparableTimeMark)

internal val ExpiringValue<Credentials>.isExpired: Boolean get() = expiresAt.hasPassedNow()

internal fun ExpiringValue<Credentials>.isExpiringWithin(duration: Duration) = (expiresAt - duration).hasPassedNow()

internal typealias S3ExpressCredentialsCacheEntry = Map.Entry<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>
