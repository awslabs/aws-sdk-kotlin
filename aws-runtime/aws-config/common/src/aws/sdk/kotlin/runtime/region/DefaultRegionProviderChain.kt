/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * [RegionProvider] that looks for region in this order:
 *  1. Check `aws.region` system property (JVM only)
 *  2. Check the `AWS_REGION` environment variable (JVM, Node, Native)
 *  3. Check the AWS config files/profile for region information
 *  4. If running on EC2, check the EC2 metadata service for region
 */
public expect class DefaultRegionProviderChain constructor(
    platformProvider: PlatformProvider = PlatformProvider.System,
    imdsClient: Lazy<InstanceMetadataProvider> = lazy { ImdsClient() },
    profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(platformProvider).activeProfile },
) : RegionProvider,
    Closeable {
    override fun close()
    override suspend fun getRegion(): String?
}
