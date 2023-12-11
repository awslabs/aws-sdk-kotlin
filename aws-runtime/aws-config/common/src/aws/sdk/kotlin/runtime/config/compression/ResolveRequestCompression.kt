/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.compression

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.disableRequestCompression
import aws.sdk.kotlin.runtime.config.profile.requestMinCompressionSizeBytes
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.http.config.RequestCompressionConfig
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve disableRequestCompression & requestMinCompressionSizeBytes from the specified sources.
 * @return The disableRequestCompression & requestMinCompressionSizeBytes settings if found, the default value for each if not.
 */
@InternalSdkApi
public suspend fun resolveRequestCompression(
    platform: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile>,
): RequestCompressionConfig.Builder =
    RequestCompressionConfig.Builder().apply {
        disableRequestCompression =
            CompressionSettings.AwsDisableRequestCompression.resolve(platform)
                ?: profile.get().disableRequestCompression
                ?: false
        requestMinCompressionSizeBytes =
            CompressionSettings.AwsRequestMinCompressionSizeBytes.resolve(platform)
                ?: profile.get().requestMinCompressionSizeBytes
                ?: 10240
    }
