/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.identity.CloseableTokenProvider
import aws.smithy.kotlin.runtime.identity.Token
import aws.smithy.kotlin.runtime.identity.TokenProviderChain
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.Attributes
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Default AWS bearer token provider chain used by services marked with [@httpBearerAuth](https://smithy.io/2.0/spec/authentication-traits.html#smithy-api-httpbearerauth-trait)
 *
 * Resolution order:
 *
 * 1. Profile ([ProfileTokenProvider]
 *
 * Closing the chain will close all child providers that implement [Closeable].
 *
 * @param profileName Override the profile name to use. If not provided it will be resolved internally
 * via environment (see [AwsSdkSetting.AwsProfile]) or defaulted to `default` if not configured.
 * @param platformProvider The platform API provider
 * @param httpClientEngine the [HttpClientEngine] instance to use to make requests. NOTE: This engine's resources and lifetime
 * are NOT managed by the provider. Caller is responsible for closing.
 */
public class DefaultChainBearerTokenProvider(
    private val profileName: String? = null,
    private val platformProvider: PlatformProvider = PlatformProvider.System,
    httpClientEngine: HttpClientEngine? = null,
) : CloseableTokenProvider {

    private val chain = TokenProviderChain(
        ProfileTokenProvider(profileName, platformProvider, httpClientEngine),
    )

    override suspend fun resolve(attributes: Attributes): Token = chain.resolve(attributes)

    override fun close() {
        chain.close()
    }
}
