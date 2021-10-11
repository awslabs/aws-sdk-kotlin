/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.region

import aws.sdk.kotlin.runtime.region.AwsRegionProvider
import aws.sdk.kotlin.runtime.region.AwsRegionProviderChain

public actual class DefaultAwsRegionProviderChain public actual constructor() :
    AwsRegionProvider,
    AwsRegionProviderChain(
        JvmSystemPropRegionProvider(),
        EnvironmentRegionProvider()
    )
