/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.regions.providers

import software.aws.clientrt.util.Platform

private const val AWS_ENVIRON_REGION = "AWS_REGION"
private const val AWS_ENVIRON_DEFAULT_REGION = "AWS_DEFAULT_REGION"

/**
 * Provide a mapping from key to value
 */
public fun interface Environment {
    public fun get(key: String): String?
}

/**
 * [AwsRegionProvider] that checks `AWS_REGION` and `AWS_DEFAULT` region environment variables
 * @param environ the environment mapping to lookup keys in (defaults to the system environment)
 */
public class EnvironmentRegionProvider(
    private val environ: Environment
) : AwsRegionProvider {
    public constructor() : this(SystemEnvironment)

    override suspend fun getRegion(): String? =
        environ.get(AWS_ENVIRON_REGION) ?: environ.get(AWS_ENVIRON_DEFAULT_REGION)
}

private object SystemEnvironment : Environment {
    override fun get(key: String): String? = Platform.getenv(key)
}
