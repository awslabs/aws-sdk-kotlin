/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider

// TODO - update these docs
// TODO - remove CRT references here and build file
// TODO - allow region, profile, etc to be passed in

/**
 * Creates the default provider chain used by most AWS SDKs.
 *
 * Generally:
 *
 * (1) Environment
 * (2) Profile
 * (3) (conditional, off by default) ECS
 * (4) (conditional, on by default) EC2 Instance Metadata
 *
 * Support for environmental control of the default provider chain is not yet implemented.
 *
 * @return the newly-constructed credentials provider
 */
public class DefaultChainCredentialsProvider internal constructor(
    private val platformProvider: PlatformProvider = Platform,
    private val httpClientEngine: HttpClientEngine? = null
) : CredentialsProvider, Closeable {

    public constructor() : this(Platform, CrtHttpEngine())

    private val chain = CredentialsProviderChain(
        EnvironmentCredentialsProvider(platformProvider::getenv),
        ProfileCredentialsProvider(platform = platformProvider, httpClientEngine = httpClientEngine),
        // TODO - explicitly add Sts and StsWebIdentity since they can be configured via environment
        EcsCredentialsProvider(platformProvider, httpClientEngine),
        ImdsCredentialsProvider(
            client = lazy {
                ImdsClient {
                    platformProvider = this@DefaultChainCredentialsProvider.platformProvider
                    engine = httpClientEngine
                }
            },
            platformProvider = platformProvider
        )
    )

    private val provider = CachedCredentialsProvider(chain)

    override suspend fun getCredentials(): Credentials = provider.getCredentials()

    override fun close() {
        chain.close()
    }
}
