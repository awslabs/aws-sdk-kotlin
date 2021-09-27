/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.build
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import aws.sdk.kotlin.crt.auth.credentials.CachedCredentialsProvider as CachedCredentialsProviderCrt

/**
 * Creates a provider that functions as a caching decorating of another provider.
 *
 * Credentials sourced through this provider will be cached within it until their expiration time.
 * When the cached credentials expire, new credentials will be fetched when next queried.
 *
 * For example, the default chain is implemented as:
 *
 * CachedProvider -> ProviderChain(EnvironmentProvider -> ProfileProvider -> ECS/EC2IMD etc...)
 *
 * @return the newly-constructed credentials provider
 */
public class CachedCredentialsProvider private constructor(builder: Builder) : CrtCredentialsProvider {

    @OptIn(ExperimentalTime::class)
    override val crtProvider: CachedCredentialsProviderCrt = CachedCredentialsProviderCrt.build {
        refreshTimeInMilliseconds = builder.refreshTime.inWholeMilliseconds

        // FIXME - note this won't work until https://github.com/awslabs/aws-crt-java/issues/252 is resolved
        source = builder.source?.let { CredentialsProviderCrtProxy(it) }
    }

    public companion object {
        /**
         * Construct a new [CachedCredentialsProvider] using the given [block]
         */
        public fun build(block: Builder.() -> Unit): CachedCredentialsProvider = Builder().apply(block).build()
    }

    @OptIn(ExperimentalTime::class)
    public class Builder {
        /**
         * The provider to cache credentials query results from
         */
        public var source: CredentialsProvider? = null

        /**
         * An optional expiration time period for sourced credentials.  For a given set of cached credentials,
         * the refresh time period will be the minimum of this time and any expiration timestamp on the credentials
         * themselves.
         */
        public var refreshTime: Duration = Duration.ZERO

        public fun build(): CachedCredentialsProvider {

            requireNotNull(source) { "CachedCredentialsProvider requires a source provider to wrap" }
            return CachedCredentialsProvider(this)
        }
    }
}
