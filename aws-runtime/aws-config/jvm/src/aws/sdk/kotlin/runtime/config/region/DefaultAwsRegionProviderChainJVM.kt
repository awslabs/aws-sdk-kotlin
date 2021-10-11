/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.region

import aws.sdk.kotlin.runtime.region.RegionProvider
import aws.sdk.kotlin.runtime.region.RegionProviderChain

public actual class DefaultRegionProviderChain public actual constructor() :
    RegionProvider,
    RegionProviderChain(
        JvmSystemPropRegionProvider(),
        EnvironmentRegionProvider()
    )
