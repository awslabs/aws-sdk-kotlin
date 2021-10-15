/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.smithy.kotlin.runtime.io.Closeable

public actual class DefaultRegionProviderChain public actual constructor() :
    RegionProvider,
    Closeable,
    RegionProviderChain(
        JvmSystemPropRegionProvider(),
        EnvironmentRegionProvider(),
        // TODO - profile
        ImdsRegionProvider()
    ) {
    override fun close() {
        providers.forEach { provider ->
            if (provider is Closeable) provider.close()
        }
    }
}
