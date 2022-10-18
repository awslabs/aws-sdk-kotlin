/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.DefaultHttpEngine
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Default AWS credential provider chain used by most AWS SDKs.
 *
 * Resolution order:
 *
 * 1. Environment variables ([EnvironmentCredentialsProvider])
 * 2. Profile ([ProfileCredentialsProvider])
 * 3. Web Identity Tokens ([StsWebIdentityCredentialsProvider]]
 * 4. ECS (IAM roles for tasks) ([EcsCredentialsProvider])
 * 5. EC2 Instance Metadata (IMDSv2) ([ImdsCredentialsProvider])
 *
 * The chain is decorated with a [CachedCredentialsProvider].
 *
 * Closing the chain will close all child providers that implement [Closeable].
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param platformProvider The platform API provider
 * @param httpClientEngine the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 * @return the newly-constructed credentials provider
 */
public class DefaultChainCredentialsProvider constructor(
    private val profileName: String? = null,
    private val platformProvider: PlatformProvider = Platform,
    httpClientEngine: HttpClientEngine? = null,
    region: String? = null
) : CredentialsProvider, Closeable {

    private val manageEngine = httpClientEngine == null
    private val httpClientEngine = httpClientEngine ?: DefaultHttpEngine()

    private val chain = CredentialsProviderChain(
        EnvironmentCredentialsProvider(platformProvider::getenv),
        ProfileCredentialsProvider(profileName = profileName, platformProvider = platformProvider, httpClientEngine = httpClientEngine, region = region),
        // STS web identity provider can be constructed from either the profile OR 100% from the environment
        StsWebIdentityProvider(platformProvider = platformProvider, httpClientEngine = httpClientEngine),
        EcsCredentialsProvider(platformProvider, httpClientEngine),
        ImdsCredentialsProvider(
            client = lazy {
                ImdsClient {
                    platformProvider = this@DefaultChainCredentialsProvider.platformProvider
                    engine = httpClientEngine
                }
            },
            platformProvider = platformProvider,
        ),
    )

    private val provider = CachedCredentialsProvider(chain)

    override suspend fun getCredentials(): Credentials = provider.getCredentials()

    override fun close() {
        provider.close()
        if (manageEngine) {
            httpClientEngine.close()
        }
    }
}

/**
 * Wrapper around [StsWebIdentityCredentialsProvider] that delays any exceptions until [getCredentials] is invoked.
 * This allows it to be part of the default chain and any failures result in the chain to move onto the next provider.
 */
private class StsWebIdentityProvider(
    val platformProvider: PlatformProvider,
    val httpClientEngine: HttpClientEngine? = null,
) : CredentialsProvider {
    override suspend fun getCredentials(): Credentials {
        val wrapped = StsWebIdentityCredentialsProvider.fromEnvironment(platformProvider = platformProvider, httpClientEngine = httpClientEngine)
        return wrapped.getCredentials()
    }
}
