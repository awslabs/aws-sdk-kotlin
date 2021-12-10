/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.CachedValue
import aws.sdk.kotlin.runtime.config.ExpiringValue
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private const val DEFAULT_CREDENTIALS_REFRESH_BUFFER_SECONDS = 10
/**
 * The amount of time credentials are valid for before being refreshed when an explicit value
 * is not given to/from a provider
 */
public const val DEFAULT_CREDENTIALS_REFRESH_SECONDS: Int = 60 * 15

/**
 * Creates a provider that functions as a caching decorator of another provider.
 *
 * Credentials sourced through this provider will be cached within it until their expiration time.
 * When the cached credentials expire, new credentials will be fetched when next queried.
 *
 * For example, the default chain is implemented as:
 *
 * CachedProvider -> ProviderChain(EnvironmentProvider -> ProfileProvider -> ECS/EC2IMD etc...)
 *
 * @param source the provider to cache credentials results from
 * @param expireCredentialsAfter the default expiration time period for sourced credentials. For a given set of
 * cached credentials, the refresh time period will be the minimum of this time and any expiration timestamp on
 * the credentials themselves.
 * @param refreshBufferWindow amount of time before the actual credential expiration time when credentials are
 * considered expired. For example, if credentials are expiring in 15 minutes, and the buffer time is 10 seconds,
 * then any requests made after 14 minutes and 50 seconds will load new credentials. Defaults to 10 seconds.
 * @param clock the source of time for this provider
 *
 * @return the newly-constructed credentials provider
 */
@OptIn(ExperimentalTime::class)
public class CachedCredentialsProvider(
    private val source: CredentialsProvider,
    private val expireCredentialsAfter: Duration = Duration.seconds(DEFAULT_CREDENTIALS_REFRESH_SECONDS),
    refreshBufferWindow: Duration = Duration.seconds(DEFAULT_CREDENTIALS_REFRESH_BUFFER_SECONDS),
    private val clock: Clock = Clock.System
) : CredentialsProvider {

    private var cachedCredentials = CachedValue<Credentials>(null, bufferTime = refreshBufferWindow, clock)

    override suspend fun getCredentials(): Credentials = cachedCredentials.getOrLoad {
        val logger = Logger.getLogger<CachedCredentialsProvider>()
        logger.trace { "refreshing credentials cache" }
        val providerCreds = source.getCredentials()
        if (providerCreds.expiration != null) {
            ExpiringValue(providerCreds, providerCreds.expiration!!)
        } else {
            val expiration = clock.now() + expireCredentialsAfter
            val creds = providerCreds.copy(expiration = expiration)
            ExpiringValue(creds, expiration)
        }
    }
}
