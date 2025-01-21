/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.client.region.RegionProvider
import aws.smithy.kotlin.runtime.util.EnvironmentProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * [RegionProvider] that checks `AWS_REGION` region environment variable
 * @param environ the environment mapping to lookup keys in (defaults to the system environment)
 */
public class EnvironmentRegionProvider(
    private val environ: EnvironmentProvider = PlatformProvider.System,
) : RegionProvider {
    override suspend fun getRegion(): String? = environ.getenv(AwsSdkSetting.AwsRegion.envVar)
}
