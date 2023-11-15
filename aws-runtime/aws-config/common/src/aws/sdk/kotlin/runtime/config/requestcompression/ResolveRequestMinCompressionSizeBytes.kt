/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.requestcompression

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.requestMinCompressionSizeBytes
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve requestMinCompressionSizeBytes from specified sources.
 * @return The requestMinCompressionSizeBytes setting if found, null if not
 */
@InternalSdkApi
public suspend fun resolveRequestMinCompressionSizeBytes(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): Int? =
        AwsSdkSetting.AwsRequestMinCompressionSizeBytes.resolve(platform) ?: profile.get().requestMinCompressionSizeBytes