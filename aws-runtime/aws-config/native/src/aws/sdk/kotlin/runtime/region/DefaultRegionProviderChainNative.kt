/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

public actual class DefaultRegionProviderChain actual constructor(
    platformProvider: PlatformProvider,
    imdsClient: Lazy<InstanceMetadataProvider>,
    profile: LazyAsyncValue<AwsProfile>,
) : RegionProvider,
    Closeable {
    actual override suspend fun getRegion(): String? {
        TODO("Not yet implemented")
    }

    actual override fun close() {
        TODO("Not yet implemented")
    }
}
