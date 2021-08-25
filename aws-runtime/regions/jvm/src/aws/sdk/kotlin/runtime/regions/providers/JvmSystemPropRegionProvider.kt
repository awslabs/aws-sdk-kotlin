/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

import aws.sdk.kotlin.runtime.AwsSdkSetting

/**
 * [AwsRegionProvider] that checks `aws.region` system property
 */
internal class JvmSystemPropRegionProvider : AwsRegionProvider {
    override suspend fun getRegion(): String? = System.getProperty(AwsSdkSetting.AwsRegion.jvmProperty, null)
}
