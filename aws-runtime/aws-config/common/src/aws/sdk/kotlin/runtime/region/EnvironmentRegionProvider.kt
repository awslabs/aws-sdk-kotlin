/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.util.EnvironmentProvider
import aws.smithy.kotlin.runtime.util.Platform

/**
 * [RegionProvider] that checks `AWS_REGION` region environment variable
 * @param environ the environment mapping to lookup keys in (defaults to the system environment)
 */
internal class EnvironmentRegionProvider(
    private val environ: EnvironmentProvider = Platform
) : RegionProvider {
    override suspend fun getRegion(): String? = environ.getenv(AwsSdkSetting.AwsRegion.environmentVariable)
}
