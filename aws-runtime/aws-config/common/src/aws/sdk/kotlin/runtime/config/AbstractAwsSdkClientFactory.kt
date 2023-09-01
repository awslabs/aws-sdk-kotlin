/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.client.AwsSdkClientConfig
import aws.sdk.kotlin.runtime.config.endpoints.resolveUseDualStack
import aws.sdk.kotlin.runtime.config.endpoints.resolveUseFips
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.retries.resolveRetryStrategy
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.client.RetryStrategyClientConfig
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.client.SdkClientConfig
import aws.smithy.kotlin.runtime.client.SdkClientFactory
import aws.smithy.kotlin.runtime.client.config.ClientSettings
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.telemetry.TelemetryConfig
import aws.smithy.kotlin.runtime.telemetry.TelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.trace.withSpan
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Abstract base class all AWS client companion objects inherit from
 *
 * @param TConfig the type of the service client configuration
 * @param TConfigBuilder the type of builder that creates [TConfig] instances
 * @param TClient the type of service client
 * @param TClientBuilder the type of builder that creates [TClient] instances
 */
public abstract class AbstractAwsSdkClientFactory<
    TConfig,
    TConfigBuilder,
    TClient : SdkClient,
    TClientBuilder : SdkClient.Builder<TConfig, TConfigBuilder, TClient>,
    > : SdkClientFactory<TConfig, TConfigBuilder, TClient, TClientBuilder>
    where TConfig : SdkClientConfig,
          TConfig : AwsSdkClientConfig,
          TConfigBuilder : SdkClientConfig.Builder<TConfig>,
          TConfigBuilder : AwsSdkClientConfig.Builder {
    /**
     * Construct a [TClient] by resolving the configuration from the current environment.
     */
    @OptIn(ExperimentalApi::class)
    public suspend fun fromEnvironment(block: (TConfigBuilder.() -> Unit)? = null): TClient {
        val builder = builder()
        val config = builder.config

        // FIXME - use a default telemetry provider that at least wires up SLF4j logging
        val telemetryProvider = (builder as? TelemetryConfig.Builder)?.telemetryProvider ?: TelemetryProvider.None
        val tracer = telemetryProvider.tracerProvider.getOrCreateTracer("AwsSdkClientFactory")

        tracer.withSpan("fromEnvironment") {
            val sharedConfig = asyncLazy { loadAwsSharedConfig(PlatformProvider.System) }
            val profile = asyncLazy { sharedConfig.get().activeProfile }

            // As a DslBuilderProperty, the value of retryStrategy cannot be checked for nullability because it may have
            // been set using a DSL. Thus, set the resolved strategy _first_ to ensure it's used as the fallback.
            if (config is RetryStrategyClientConfig.Builder) {
                config.retryStrategy = resolveRetryStrategy(profile = profile)
            }

            if (block != null) {
                config.apply(block)
            }

            config.logMode = config.logMode ?: ClientSettings.LogMode.resolve(platform = PlatformProvider.System)
            config.region = config.region ?: resolveRegion(profile = profile)
            config.useFips = config.useFips ?: resolveUseFips(profile = profile)
            config.useDualStack = config.useDualStack ?: resolveUseDualStack(profile = profile)
            finalizeConfig(builder, sharedConfig)
        }
        return builder.build()
    }

    /**
     * Inject any client-specific config.
     */
    protected open suspend fun finalizeConfig(builder: TClientBuilder, sharedConfig: LazyAsyncValue<AwsSharedConfig>) { }
}
