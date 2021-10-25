/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.imds.InstanceMetadataProvider
import aws.smithy.kotlin.runtime.io.Closeable
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal actual class DefaultRegionProviderChain actual constructor(
    platformProvider: PlatformProvider,
    imdsClient: Lazy<InstanceMetadataProvider>,
) : RegionProvider,
    Closeable,
    RegionProviderChain(
        JvmSystemPropRegionProvider(platformProvider),
        EnvironmentRegionProvider(platformProvider),
        ProfileRegionProvider(platformProvider),
        ImdsRegionProvider(client = imdsClient, platformProvider = platformProvider)
    ) {

    override fun close() {
        providers.forEach { provider ->
            if (provider is Closeable) provider.close()
        }
    }
}
