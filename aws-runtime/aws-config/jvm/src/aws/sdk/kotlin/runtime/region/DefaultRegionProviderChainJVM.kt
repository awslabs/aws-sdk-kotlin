/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.smithy.kotlin.runtime.util.PlatformProvider

public actual class DefaultRegionProviderChain public actual constructor(platformProvider: PlatformProvider) :
    RegionProvider,
    RegionProviderChain(
        JvmSystemPropRegionProvider(platformProvider),
        EnvironmentRegionProvider(platformProvider),
        ProfileRegionProvider(platformProvider)
    )
