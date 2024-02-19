/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.sdk.kotlin.services.s3.S3Attributes
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.*
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.io.SdkManagedBase
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.getLogger
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.until
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal val REFRESH_BUFFER = 1.minutes
private val DEFAULT_REFRESH_PERIOD = 3.minutes
private const val CREDENTIALS_PROVIDER_NAME = "DefaultS3ExpressCredentialsProvider"

internal class DefaultS3ExpressCredentialsProvider(
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val clock: Clock = Clock.System,
) : S3ExpressCredentialsProvider, SdkManagedBase(), CoroutineScope {
    private lateinit var client: S3Client
    private lateinit var logger: Logger
    private val credentialsCache = S3ExpressCredentialsCache()

    override val coroutineContext: CoroutineContext = Job() +
        CoroutineName(CREDENTIALS_PROVIDER_NAME)

    init {
        launch(coroutineContext) {
            refresh()
        }
    }

    @OptIn(ExperimentalApi::class)
    override suspend fun resolve(attributes: Attributes): Credentials {
        client = (attributes[S3Attributes.ExpressClient] as S3Client)
        logger = client.config.telemetryProvider.loggerProvider.getLogger<S3ExpressCredentialsProvider>()

        val key = S3ExpressCredentialsCacheKey(attributes[S3Attributes.Bucket], client.config.credentialsProvider.resolve(attributes))

        return credentialsCache.get(key)?.takeIf { !it.isExpired }?.value
            ?: createSessionCredentials(key.bucket).also { credentialsCache.put(key, it) }.value
    }

    override fun close() = coroutineContext.cancel(null)


    /**
     * Attempt to refresh the credentials in the cache. A refresh is initiated when the `nextRefresh` time has been reached,
     * which is either `DEFAULT_REFRESH_PERIOD` or the soonest credentials expiration time (minus a buffer), whichever comes first.
     */
    private suspend fun refresh() {
        while (isActive) {
            if (!this::client.isInitialized) {
                delay(5.seconds)
                continue
            } else if (credentialsCache.size == 0) {
                logger.trace { "Cache is empty, waiting..." }
                delay(5.seconds)
                continue
            }

            val entries = credentialsCache.entries

            // Evict any credentials that weren't used since the last refresh
            entries.filter { !it.value.usedSinceLastRefresh }.forEach {
                logger.debug { "Credentials for ${it.key.bucket} were not used since last refresh, evicting..." }
                credentialsCache.remove(it.key)
            }

            // Mark all credentials as not used since last refresh
            entries.forEach {
                it.value.usedSinceLastRefresh = false
            }

            // Refresh any credentials which are already expired
            val expiredEntries = entries.filter { it.value.expiringCredentials.isExpired }

            supervisorScope {
                expiredEntries.forEach { entry ->
                    logger.debug { "Credentials for ${entry.key.bucket} are expired, refreshing..." }

                    try {
                        val refreshed = async { createSessionCredentials(entry.key.bucket) }.await()
                        credentialsCache.put(entry.key, refreshed, false)
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to refresh credentials for ${entry.key.bucket}" }
                    }

                }
            }

            // Find the next expiring credentials, sleep until then
            val nextExpiringEntry = entries.maxByOrNull {
                // note: `expiresAt` is always in the future, which means the `elapsedNow` values are negative.
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
                providerName = CREDENTIALS_PROVIDER_NAME ,
            ),
            expirationTimeMark,
        )
    }
}

/**
 * Get the [Duration] between [this] TimeMark and an [other] TimeMark
 */
internal fun TimeMark.until(other: TimeMark): Duration = (this.elapsedNow().absoluteValue - other.elapsedNow().absoluteValue).absoluteValue
