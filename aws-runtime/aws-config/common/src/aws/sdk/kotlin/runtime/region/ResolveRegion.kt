/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * Attempt to resolve the region to make requests to, throws [ConfigurationException] if region could not be
 * resolved.
 */
@InternalSdkApi
public suspend fun resolveRegion(
    platformProvider: PlatformProvider = PlatformProvider.System,
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadActiveAwsProfile(platformProvider) },
): String =
    DefaultRegionProviderChain(platformProvider, profile = profile).use { providerChain ->
        providerChain.getRegion() ?: throw ConfigurationException("unable to auto detect AWS region, tried: $providerChain")
    }
