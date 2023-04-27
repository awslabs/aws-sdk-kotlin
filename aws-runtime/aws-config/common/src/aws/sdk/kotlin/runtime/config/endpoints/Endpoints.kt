/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.endpoints

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.sdk.kotlin.runtime.config.profile.useDualStack
import aws.sdk.kotlin.runtime.config.profile.useFips
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempts to resolve the enabled state of FIPS endpoints from the environment.
 */
@InternalSdkApi
public suspend fun resolveUseFips(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadActiveAwsProfile(provider) },
): Boolean? =
    AwsSdkSetting.AwsUseFipsEndpoint.resolve(provider)
        ?: profile.get().useFips

/**
 * Attempts to resolve the enabled state of dual-stack endpoints from the environment.
 */
@InternalSdkApi
public suspend fun resolveUseDualStack(
    provider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadActiveAwsProfile(provider) },
): Boolean? =
    AwsSdkSetting.AwsUseDualStackEndpoint.resolve(provider)
        ?: profile.get().useDualStack
