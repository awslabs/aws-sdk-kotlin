/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.getBooleanOrNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.util.LazyAsyncValue

internal suspend fun finalizeS3Config(builder: S3Client.Builder, sharedConfig: LazyAsyncValue<AwsSharedConfig>) {
    sharedConfig.get().activeProfile.let {
        builder.config.forcePathStyle = builder.config.forcePathStyle ?: it.forcePathStyle
        builder.config.enableAccelerate = builder.config.enableAccelerate ?: it.enableAccelerate
    }
}

private val AwsProfile.forcePathStyle: Boolean?
    get() = getOrNull("s3", "addressing_style")?.lowercase()?.let {
        when (it) {
            "virtual", "auto" -> false
            "path" -> true
            else -> throw ConfigurationException("invalid value '$it' for config property s3.addressing_style")
        }
    }

private val AwsProfile.enableAccelerate: Boolean?
    get() = getBooleanOrNull("s3", "use_accelerate_endpoint")
