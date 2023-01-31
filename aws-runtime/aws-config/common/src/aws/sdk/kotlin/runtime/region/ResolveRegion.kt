/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempt to resolve the region to make requests to, throws [ConfigurationException] if region could not be
 * resolved.
 */
@InternalSdkApi
public suspend fun resolveRegion(
    platformProvider: PlatformProvider = PlatformProvider.System,
): String =
    DefaultRegionProviderChain(platformProvider).use { providerChain ->
        providerChain.getRegion() ?: throw ConfigurationException("unable to auto detect AWS region, tried: $providerChain")
    }
