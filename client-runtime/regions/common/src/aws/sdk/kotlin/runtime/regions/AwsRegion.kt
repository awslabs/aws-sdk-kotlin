/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions

import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.client.AwsAdvancedClientOption
import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.regions.providers.DefaultAwsRegionProviderChain
import software.aws.clientrt.client.ExecutionContext

/**
 * Attempt to resolve the region to make requests to.
 *
 * Regions are resolved in the following order:
 *   1. From the existing [ctx]
 *   2. From the region [config]
 *   3. Using default region detection (only if-enabled)
 */
@InternalSdkApi
public suspend fun resolveRegionForOperation(ctx: ExecutionContext, config: RegionConfig): String {
    // favor the context if it's already set
    val regionFromCtx = ctx.getOrNull(AwsClientOption.Region)
    if (regionFromCtx != null) return regionFromCtx

    // use the default from the service config if configured
    if (config.region != null) return config.region!!

    // attempt to detect
    val allowDefaultRegionDetect = ctx.getOrNull(AwsAdvancedClientOption.EnableDefaultRegionDetection) ?: true
    if (!allowDefaultRegionDetect) {
        throw ClientException("No region was configured and region detection has been disabled")
    }

    // TODO - propagate any relevant ctx/config to the default chain
    return DefaultAwsRegionProviderChain().getRegion() ?: throw ClientException("unable to auto detect AWS region")
}
