/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.sdk.kotlin.runtime.config.profile.maxAttempts
import aws.sdk.kotlin.runtime.config.profile.retryMode
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.retries.RetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategyOptions
import aws.smithy.kotlin.runtime.util.Platform
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempt to resolve the retry strategy used to make requests by fetching the max attempts and retry mode. Currently,
 * we only support the legacy and standard retry modes.
 */
@InternalSdkApi
public suspend fun resolveRetryStrategy(platformProvider: PlatformProvider = Platform): RetryStrategy {
    val profile = asyncLazy { loadActiveAwsProfile(platformProvider) }

    val maxAttempts = AwsSdkSetting.AwsMaxAttempts.resolve(platformProvider)
        ?: profile.get().maxAttempts
        ?: StandardRetryStrategyOptions.Default.maxAttempts

    if (maxAttempts < 1) { throw ConfigurationException("max attempts was $maxAttempts, but should be at least 1") }

    val retryMode = AwsSdkSetting.AwsRetryMode.resolve(platformProvider)
        ?: profile.get().retryMode
        ?: RetryMode.STANDARD

    return when (retryMode) {
        RetryMode.STANDARD, RetryMode.LEGACY -> StandardRetryStrategy(StandardRetryStrategyOptions(maxAttempts))
        RetryMode.ADAPTIVE -> throw NotImplementedError("Retry mode $retryMode is not implemented yet. https://github.com/awslabs/aws-sdk-kotlin/issues/701")
    }
}
