/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.imds.ImdsClient
import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.PlatformEnvironProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.asyncLazy

private const val REGION_PATH: String = "/latest/meta-data/placement/region"

/**
 * [RegionProvider] that uses EC2 instance metadata service (IMDS) to provider region information
 *
 * @param client the IMDS client to use to resolve region information with
 * @param platformProvider the [PlatformEnvironProvider] instance
 */
public class ImdsRegionProvider(
    private val client: Lazy<InstanceMetadataProvider> = lazy { ImdsClient() },
    private val platformProvider: PlatformEnvironProvider = PlatformProvider.System,
) : RegionProvider,
    Closeable {
    private val resolvedRegion = asyncLazy(::loadRegion)

    override suspend fun getRegion(): String? {
        if (AwsSdkSetting.AwsEc2MetadataDisabled.resolve(platformProvider) == true) {
            return null
        }

        return resolvedRegion.get()
    }

    private suspend fun loadRegion(): String = client.value.get(REGION_PATH)

    override fun close() {
        if (client.isInitialized()) {
            client.value.close()
        }
    }
}
