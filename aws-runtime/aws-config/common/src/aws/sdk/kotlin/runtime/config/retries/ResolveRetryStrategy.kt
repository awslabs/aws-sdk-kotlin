/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.retries

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.sdk.kotlin.runtime.config.profile.maxRetryAttempts
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

    // resolve max attempts
    val maxAttempts: Int = (
        AwsSdkSetting.AwsMaxAttempts.resolve(platformProvider)
            ?: profile.get().maxRetryAttempts
            ?: (StandardRetryStrategyOptions.Default.maxAttempts).toString()
        ).toInt()

    if (maxAttempts < 1) {
        throw IllegalArgumentException("max attempts was $maxAttempts but should be at least 1")
    }

    // resolve retry mode
    val retryMode = AwsSdkSetting.AwsRetryMode.resolve(platformProvider) ?: profile.get().retryMode ?: "standard"
    if (retryMode == "adaptive") {
        throw NotImplementedError("Retry mode $retryMode is not implemented yet. https://github.com/awslabs/aws-sdk-kotlin/issues/701")
    } else if (retryMode != "legacy" && retryMode != "standard") {
        throw UnsupportedOperationException("Retry mode $retryMode is not supported, should be one of: \"legacy\", \"standard\", \"adaptive\"")
    }

    return StandardRetryStrategy(StandardRetryStrategyOptions(maxAttempts))
}
