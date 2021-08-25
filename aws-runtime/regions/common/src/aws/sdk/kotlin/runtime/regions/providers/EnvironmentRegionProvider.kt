/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

import aws.sdk.kotlin.runtime.AwsSdkSetting
import aws.smithy.kotlin.runtime.util.Platform

/**
 * Provide a mapping from key to value
 */
public fun interface Environment {
    public fun get(key: String): String?
}

/**
 * [AwsRegionProvider] that checks `AWS_REGION` region environment variable
 * @param environ the environment mapping to lookup keys in (defaults to the system environment)
 */
public class EnvironmentRegionProvider(
    private val environ: Environment
) : AwsRegionProvider {
    public constructor() : this(Platform::getenv)

    override suspend fun getRegion(): String? = environ.get(AwsSdkSetting.AwsRegion.environmentVariable)
}
