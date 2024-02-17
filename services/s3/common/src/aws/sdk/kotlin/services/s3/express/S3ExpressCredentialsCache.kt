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
private val REFRESH_BUFFER = 1.minutes
private val DEFAULT_REFRESH_PERIOD = 3.minutes

internal class S3ExpressCredentialsCache(
    private val client: S3Client,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val clock: Clock = Clock.System,
) : CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext = Job() + CoroutineName("S3ExpressCredentialsCacheRefresh") + TelemetryProviderContext(client.config.telemetryProvider)

    private val lru = LruCache<S3ExpressCredentialsCacheKey, S3ExpressCredentialsCacheValue>(DEFAULT_S3_EXPRESS_CACHE_SIZE)

    init {
        launch(coroutineContext) {
            refresh()
        }
    }

    suspend fun get(key: S3ExpressCredentialsCacheKey): Credentials = lru
        .get(key)
        ?.takeIf { !it.expiringCredentials.isExpired }
        ?.let {
            it.usedSinceLastRefresh = true
            it.expiringCredentials.value
        }
        ?: (createSessionCredentials(key.bucket).also { put(key, it) }).value

    suspend fun put(key: S3ExpressCredentialsCacheKey, value: ExpiringValue<Credentials>) {
        lru.put(key, S3ExpressCredentialsCacheValue(value, usedSinceLastRefresh = true))
    }

    private val ExpiringValue<Credentials>.isExpired: Boolean get() = (expiresAt + REFRESH_BUFFER).hasPassedNow()

    /**
     * Attempt to refresh the credentials in the cache. A refresh is initiated when the `nextRefresh` time has been reached,
     * which is either `DEFAULT_REFRESH_PERIOD` or the soonest credentials expiration time (minus a buffer), whichever comes first.
     */
    private suspend fun refresh() {
        val logger = coroutineContext.logger<S3ExpressCredentialsCache>()
        while (isActive) {
            if (lru.size == 0) {
                logger.trace { "Cache is empty, waiting..." }
                delay(5.seconds)
                continue
            }

            val entries = lru.entries

            // Evict any credentials that weren't used since the last refresh
            entries.filter { !it.value.usedSinceLastRefresh }.forEach {
                logger.debug { "Credentials for ${it.key.bucket} were not used since last refresh, evicting..." }
                lru.remove(it.key)
            }

            // Mark all credentials as not used since last refresh
            entries.forEach {
                it.value.usedSinceLastRefresh = false
            }

            // Refresh any credentials which are already expired
            val expiredEntries = entries.filter { it.value.expiringCredentials.isExpired }
            expiredEntries.forEach { entry ->
                logger.debug { "Credentials for ${entry.key.bucket} are expired, refreshing..." }
                lru.put(entry.key, S3ExpressCredentialsCacheValue(createSessionCredentials(entry.key.bucket), false))
            }

            // Find the next expiring credentials, sleep until then
            val nextExpiringEntry = entries.maxByOrNull {
                // note: `expiresAt` is always in the future, so the `elapsedNow` values are negative.
                // that's the reason `maxBy` is used instead of `minBy`
                it.value.expiringCredentials.expiresAt.elapsedNow()
            }

            val delayDuration = nextExpiringEntry
                ?.let { timeSource.markNow().until(it.value.expiringCredentials.expiresAt) }
                ?: DEFAULT_REFRESH_PERIOD

            logger.debug { "Completed credentials refresh, next attempt in $delayDuration" }
            delay(delayDuration)
        }
    }

    private suspend fun createSessionCredentials(bucket: String): ExpiringValue<Credentials> {
        val credentials = client.createSession { this.bucket = bucket }.credentials!!
        val expirationTimeMark = timeSource.markNow() + clock.now().until(credentials.expiration)

        return ExpiringValue(
            Credentials(
                accessKeyId = credentials.accessKeyId,
                secretAccessKey = credentials.secretAccessKey,
                sessionToken = credentials.sessionToken,
                expiration = credentials.expiration,
                providerName = "DefaultS3ExpressCredentialsProvider",
            ),
            expirationTimeMark,
        )
    }

    override fun close() {
        coroutineContext.cancel(null)
    }
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
 * Get the [Duration] between [this] TimeMark and an [other] TimeMark
 */
internal fun TimeMark.until(other: TimeMark): Duration = (this.elapsedNow().absoluteValue - other.elapsedNow().absoluteValue).absoluteValue

/**
 * A value with an expiration [TimeMark]
 */
internal data class ExpiringValue<T> (val value: T, val expiresAt: TimeMark)
