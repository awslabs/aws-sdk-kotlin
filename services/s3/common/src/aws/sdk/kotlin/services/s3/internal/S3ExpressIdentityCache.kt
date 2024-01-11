package aws.sdk.kotlin.runtime.auth.credentials.internal

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateSessionRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.collections.LruCache
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.util.ExpiringValue
import kotlin.coroutines.coroutineContext

private const val DEFAULT_S3_EXPRESS_CACHE_SIZE: Int = 100

public class S3ExpressCredentialsCache(
    private val clock: Clock = Clock.System
) {
    private val lru = LruCache<S3ExpressCredentialsCacheKey, ExpiringValue<Credentials>>(DEFAULT_S3_EXPRESS_CACHE_SIZE)

    public suspend fun get(key: S3ExpressCredentialsCacheKey): Credentials {
        val logger = coroutineContext.logger<S3ExpressCredentialsCache>()

        val cached = lru.get(key)

        if (cached == null) {
            logger.debug { "could not find cache value for key $key. entries: ${lru.entries.joinToString { it.key.toString() }}" }
        } else if (cached.expiresAt > clock.now()) {
            logger.debug { "found expired cache value for key $key: ${cached.value}" }
        } else {
            logger.debug { "got cached value ${cached.value}" }
        }

        if (cached == null || cached.expiresAt > clock.now()) {
            val newCredentials = createSessionCredentials(key)

            lru.put(key, newCredentials)
            logger.debug { "fetched new credentials ${newCredentials.value}. entries: ${lru.entries.joinToString { it.key.toString() }}" }
            return newCredentials.value
        }

        return cached.value
    }

    private suspend fun createSessionCredentials(key: S3ExpressCredentialsCacheKey): ExpiringValue<Credentials> {
        val logger = coroutineContext.logger<S3ExpressCredentialsCache>()

        val credentials = (key.client as S3Client).createSession(CreateSessionRequest {
            bucket = key.bucket
        }).credentials!!

        return ExpiringValue(
            credentials(
                accessKeyId = credentials.accessKeyId,
                secretAccessKey = credentials.secretAccessKey,
                sessionToken = credentials.sessionToken,
                expiration = credentials.expiration,
                providerName = "S3ExpressCredentialsProvider"
            ),
            credentials.expiration
        ).also { logger.debug { "got credentials ${it.value}" } }
    }
}

public class S3ExpressCredentialsCacheKey(
    public val bucket: String,
    public val client: SdkClient,
    public val credentials: Credentials
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