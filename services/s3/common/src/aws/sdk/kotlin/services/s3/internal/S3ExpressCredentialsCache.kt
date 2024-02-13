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

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100
private val REFRESH_BUFFER = 1.minutes
private val DEFAULT_REFRESH_PERIOD = 5.minutes

internal class S3ExpressCredentialsCache(
    private val clock: Clock = Clock.System,
) : CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext = Job() + CoroutineName("S3ExpressCredentialsCacheRefresh")

    private val lru = LruCache<S3ExpressCredentialsCacheKey, ExpiringValue<Credentials>>(DEFAULT_S3_EXPRESS_CACHE_SIZE)
    private val immediateRefreshChannel = Channel<Unit>(Channel.CONFLATED) // channel used to indicate an immediate refresh attempt is required

    init {
        launch(coroutineContext) {
            refresh()
        }
    }

    suspend fun get(key: S3ExpressCredentialsCacheKey): Credentials = lru.get(key)?.takeIf { !it.isExpired(clock) }?.value
        ?: (createSessionCredentials(key).also { put(key, it) }).value

    suspend fun put(key: S3ExpressCredentialsCacheKey, value: ExpiringValue<Credentials>) {
        lru.put(key, value)
        immediateRefreshChannel.send(Unit)
    }

    private fun ExpiringValue<Credentials>.isExpired(clock: Clock): Boolean = clock.now().until(expiresAt).absoluteValue <= REFRESH_BUFFER

    /**
     * Attempt to refresh the credentials in the cache. A refresh is initiated when:
     *    * a new set of credentials are added to the cache (immediate refresh)
     *    * the `nextRefresh` time has been reached, which is either `DEFAULT_REFRESH_PERIOD` or
     *      the soonest credentials expiration time (minus a buffer), whichever comes first.
     */
    private suspend fun refresh() {
        val logger = coroutineContext.logger<S3ExpressCredentialsCache>()
        while (isActive) {
            val refreshedCredentials = mutableMapOf<S3ExpressCredentialsCacheKey, ExpiringValue<Credentials>>()
            var nextRefresh: Instant = clock.now() + DEFAULT_REFRESH_PERIOD

            lru.withLock {
                lru.entries.forEach { (key, cachedValue) ->
                    nextRefresh = minOf(nextRefresh, cachedValue.expiresAt - REFRESH_BUFFER)

                    if (cachedValue.isExpired(clock)) {
                        logger.debug { "Credentials for ${key.bucket} expire within the refresh buffer period, performing a refresh..." }
                        createSessionCredentials(key).also {
                            refreshedCredentials[key] = it
                            nextRefresh = minOf(nextRefresh, it.expiresAt - REFRESH_BUFFER)
                        }
                    }
                }

                refreshedCredentials.forEach { (key, value) ->
                    lru.remove(key)
                    lru.putUnlocked(key, value)
                }
            }

            // wake up when it's time to refresh or an immediate refresh has been triggered
            select<Unit> {
                onTimeout(clock.now().until(nextRefresh)) {}
                immediateRefreshChannel.onReceive {}
            }
        }
    }

    private suspend fun createSessionCredentials(key: S3ExpressCredentialsCacheKey): ExpiringValue<Credentials> {
        val credentials = (key.client as S3Client).createSession { bucket = key.bucket }.credentials!!

        return ExpiringValue(
            Credentials(
                accessKeyId = credentials.accessKeyId,
                secretAccessKey = credentials.secretAccessKey,
                sessionToken = credentials.sessionToken,
                expiration = credentials.expiration,
                providerName = "S3ExpressCredentialsProvider",
            ),
            credentials.expiration,
        )
    }

    override fun close() {
        coroutineContext.cancel(null)
        immediateRefreshChannel.close()
    }
}

internal data class S3ExpressCredentialsCacheKey(
    val bucket: String,
    val client: SdkClient,
    val credentials: Credentials,
)
