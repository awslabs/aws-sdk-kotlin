/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.client.AwsSdkClient
import aws.sdk.kotlin.runtime.client.AwsSdkClientConfig
import aws.sdk.kotlin.runtime.config.retries.resolveRetryStrategy
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.smithy.kotlin.runtime.client.SdkClientFactory

/**
 * Abstract base class all AWS client companion objects inherit from
 */
@InternalSdkApi
public abstract class AbstractAwsSdkClientFactory<
    TConfig : AwsSdkClientConfig,
    TConfigBuilder : AwsSdkClientConfig.Builder<TConfig>,
    TClient : AwsSdkClient,
    TClientBuilder : AwsSdkClient.Builder<TConfig, TConfigBuilder, TClient>,
    > : SdkClientFactory<TConfig, TConfigBuilder, TClient, TClientBuilder> {

    /**
     * Construct a [TClient] by resolving the configuration from the current environment.
     */
    public suspend fun fromEnvironment(block: (TConfigBuilder.() -> Unit)? = null): TClient {
        val builder = builder()
        if (block != null) builder.config.apply(block)

        builder.config.region = builder.config.region ?: resolveRegion()
        builder.config.retryStrategy = builder.config.retryStrategy ?: resolveRetryStrategy()
        return builder.build()
    }
}
