/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.getBooleanOrNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.util.LazyAsyncValue

internal suspend fun finalizeS3Config(builder: S3Client.Builder, sharedConfig: LazyAsyncValue<AwsSharedConfig>) {
    sharedConfig.get().activeProfile.let {
        builder.config.useArnRegion = builder.config.useArnRegion ?: it.useArnRegion
        builder.config.disableMrap = builder.config.disableMrap ?: it.disableMrap
    }
}

private val AwsProfile.useArnRegion: Boolean?
    get() = getBooleanOrNull("s3_use_arn_region")

private val AwsProfile.disableMrap: Boolean?
    get() = getBooleanOrNull("s3_disable_multiregion_access_points")
