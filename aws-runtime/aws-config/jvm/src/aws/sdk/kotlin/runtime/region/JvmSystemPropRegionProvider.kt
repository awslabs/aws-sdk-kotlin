/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.PropertyProvider

/**
 * [RegionProvider] that checks `aws.region` system property
 */
public class JvmSystemPropRegionProvider(
    private val propertyProvider: PropertyProvider = PlatformProvider.System,
) : RegionProvider {
    override suspend fun getRegion(): String? = propertyProvider.getProperty(AwsSdkSetting.AwsRegion.sysProp)
}
