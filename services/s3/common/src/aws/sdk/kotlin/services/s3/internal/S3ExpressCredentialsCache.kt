/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.collections.LruCache
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.until
import aws.smithy.kotlin.runtime.util.ExpiringValue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100
private val REFRESH_BUFFER = 1.minutes
private val DEFAULT_REFRESH_PERIOD = 5.minutes

internal class S3ExpressCredentialsCache(
    private val client: S3Client,
    private val clock: Clock = Clock.System,
) : CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext = Job() + CoroutineName("S3ExpressCredentialsCacheRefresh")

    private val lru = LruCache<S3ExpressCredentialsCacheKey, ExpiringValue<Credentials>>(DEFAULT_S3_EXPRESS_CACHE_SIZE)

    init {
        launch(coroutineContext) {
            refresh()
        }
    }

    suspend fun get(key: S3ExpressCredentialsCacheKey): Credentials = lru.get(key)?.takeIf { !it.isExpired(clock) }?.value
        ?: (createSessionCredentials(key.bucket).also { put(key, it) }).value

    suspend fun put(key: S3ExpressCredentialsCacheKey, value: ExpiringValue<Credentials>) {
        lru.put(key, value)
    }

    private fun ExpiringValue<Credentials>.isExpired(clock: Clock): Boolean = clock.now().until(expiresAt).absoluteValue - 5.minutes <= REFRESH_BUFFER

    /**
     * Attempt to refresh the credentials in the cache. A refresh is initiated when:
     *    * a new set of credentials are added to the cache (immediate refresh)
     *    * the `nextRefresh` time has been reached, which is either `DEFAULT_REFRESH_PERIOD` or
     *      the soonest credentials expiration time (minus a buffer), whichever comes first.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun refresh() {
        while (isActive) {
            if (lru.size == 0) {
                println("CACHE: LRU is empty, continuing...")
                delay(5.seconds)
                continue
            }

            // Refresh any credentials which are already expired
            val expiredEntries = lru.entries.filter { it.value.isExpired(clock) }
            println("CACHE: ${expiredEntries.size} entries are expired, refreshing...")
            expiredEntries.forEach { entry ->
                lru.put(entry.key, createSessionCredentials(entry.key.bucket))
            }

            // Find the next expiring credentials, sleep until then
            val nextExpiringEntry = lru.entries.minBy { it.value.expiresAt }
            println("CACHE: Current time ${clock.now()}, next expiration: ${nextExpiringEntry.value.expiresAt} ")
            val nextRefresh = minOf(clock.now() + DEFAULT_REFRESH_PERIOD, nextExpiringEntry.value.expiresAt - REFRESH_BUFFER)

            select {
                onTimeout(clock.now().until(nextRefresh)) {}
            }
        }
    }

    private suspend fun createSessionCredentials(bucket: String): ExpiringValue<Credentials> {
        val credentials = client.createSession { this.bucket = bucket }.credentials!!

        return ExpiringValue(
            Credentials(
                accessKeyId = credentials.accessKeyId,
                secretAccessKey = credentials.secretAccessKey,
                sessionToken = credentials.sessionToken,
                expiration = credentials.expiration,
                providerName = "DefaultS3ExpressCredentialsProvider",
            ),
            credentials.expiration,
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
