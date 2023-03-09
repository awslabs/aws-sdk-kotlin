/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.loadActiveAwsProfile
import aws.sdk.kotlin.runtime.config.profile.region
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

/**
 * [RegionProvider] that sources region information from the active profile
 */
internal class ProfileRegionProvider(
    private val profile: LazyAsyncValue<AwsProfile> = asyncLazy { loadActiveAwsProfile(PlatformProvider.System) },
) : RegionProvider {
    override suspend fun getRegion(): String? = profile.get().region
}
