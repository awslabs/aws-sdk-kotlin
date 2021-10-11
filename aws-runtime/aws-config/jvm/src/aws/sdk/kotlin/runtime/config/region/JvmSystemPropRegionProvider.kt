/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.region

import aws.sdk.kotlin.runtime.AwsSdkSetting
import aws.sdk.kotlin.runtime.region.AwsRegionProvider

/**
 * [AwsRegionProvider] that checks `aws.region` system property
 */
internal class JvmSystemPropRegionProvider : AwsRegionProvider {
    override suspend fun getRegion(): String? = System.getProperty(AwsSdkSetting.AwsRegion.jvmProperty, null)
}
