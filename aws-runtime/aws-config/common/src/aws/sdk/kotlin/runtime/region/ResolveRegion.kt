/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempt to resolve the AWS region to which requests should be made. Returns null if none was detected.
 */
@InternalSdkApi
public suspend fun resolveRegion(
    platformProvider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(platformProvider).activeProfile },
): String? = DefaultRegionProviderChain(platformProvider, profile = profile).use { it.getRegion() }
