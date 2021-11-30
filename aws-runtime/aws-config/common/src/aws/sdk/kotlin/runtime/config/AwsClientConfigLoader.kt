/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.client.AwsClientConfig
import aws.sdk.kotlin.runtime.region.resolveRegion
import aws.smithy.kotlin.runtime.client.SdkLogMode
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Options that control how an [aws.sdk.kotlin.runtime.client.AwsClientConfig] is resolved
 */
public class AwsClientConfigLoadOptions {
    // TODO - see Go SDK for an idea of possible knobs
    //  https://github.com/aws/aws-sdk-go-v2/blob/973e5f5e9477e0690bcb4be3145bb72e956651cc/config/load_options.go#L22
    // Most of these will not be needed if you are using a corresponding environment variable or system property but there should be an explicit
    // option to control behavior.

    /**
     * The region to make requests to. When set, region will not attempt to be resolved from other sources
     */
    public var region: String? = null

    /**
     * The [CredentialsProvider] to use when sourcing [aws.sdk.kotlin.runtime.auth.credentials.Credentials].
     */
    public var credentialsProvider: CredentialsProvider? = null

    /**
     * The [SdkLogMode] to apply to service clients.
     */
    public var sdkLogMode: SdkLogMode = SdkLogMode.Default

    // FIXME - expose profile name override and thread through region/cred provider chains
}

/**
 * Load the AWS client configuration from the environment.
 */
public suspend fun AwsClientConfig.Companion.fromEnvironment(
    block: AwsClientConfigLoadOptions.() -> Unit = {}
): AwsClientConfig = loadAwsClientConfig(Platform, block)

internal suspend fun loadAwsClientConfig(
    platformProvider: PlatformProvider,
    block: AwsClientConfigLoadOptions.() -> Unit
): AwsClientConfig {
    val opts = AwsClientConfigLoadOptions().apply(block)

    val region = opts.region ?: resolveRegion(platformProvider)
    val credentialsProvider = opts.credentialsProvider ?: DefaultChainCredentialsProvider()
    val sdkLogMode = opts.sdkLogMode

    return ResolvedAwsConfig(region, credentialsProvider, sdkLogMode)
}

private data class ResolvedAwsConfig(
    override val region: String,
    override val credentialsProvider: CredentialsProvider,
    override val sdkLogMode: SdkLogMode,
) : AwsClientConfig
