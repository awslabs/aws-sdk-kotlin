/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadAwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.region
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * [RegionProvider] that sources region information from the active profile
 */
public class ProfileRegionProvider(
    private val profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadAwsSharedConfig(PlatformProvider.System).activeProfile },
) : RegionProvider {

    /**
     * Create a new [ProfileRegionProvider] that sources region from the given [profileName]
     */
    public constructor(profileName: String) : this(asyncLazy { loadAwsSharedConfig(PlatformProvider.System, profileName).activeProfile })
    override suspend fun getRegion(): String? = profile.get().region
}
