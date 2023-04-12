/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.sdk.kotlin.runtime.config.profile.sdkLogMode
import aws.smithy.kotlin.runtime.client.SdkLogMode
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempt to resolve the SDK log mode used to make requests
 */
@InternalSdkApi
public suspend fun resolveSdkLogMode(
    platformProvider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadActiveAwsProfile(platformProvider) },
): SdkLogMode = AwsSdkSetting.SdkLogMode.resolve(platformProvider) ?: profile.get().sdkLogMode ?: SdkLogMode.Default
