/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.compression

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.disableRequestCompression
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve disableRequestCompression from specified sources.
 * @return The disableRequestCompression setting if found, the default value if not
 */
@InternalSdkApi
public suspend fun resolveDisableRequestCompression(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): Boolean =
    CompressionSettings.AwsDisableRequestCompression.resolve(platform) ?: profile.get().disableRequestCompression ?: false
