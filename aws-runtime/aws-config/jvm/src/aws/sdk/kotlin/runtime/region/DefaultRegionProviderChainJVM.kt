/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

public actual class DefaultRegionProviderChain public actual constructor() :
    RegionProvider,
    RegionProviderChain(
        JvmSystemPropRegionProvider(),
        EnvironmentRegionProvider(),
        ProfileRegionProvider()
    )
