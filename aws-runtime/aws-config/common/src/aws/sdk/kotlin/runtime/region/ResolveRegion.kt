/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempt to resolve the region to make requests to.
 */
internal suspend fun resolveRegion(platformProvider: PlatformProvider): String =
    DefaultRegionProviderChain(platformProvider).use { providerChain ->
        providerChain.getRegion() ?: throw ConfigurationException("unable to auto detect AWS region, tried: $providerChain")
    }
