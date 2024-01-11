/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.AwsSharedConfig
import aws.sdk.kotlin.runtime.config.profile.getBooleanOrNull
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

internal suspend fun finalizeS3Config(
    builder: S3Client.Builder,
    sharedConfig: LazyAsyncValue<AwsSharedConfig>,
    provider: PlatformProvider = PlatformProvider.System,
) {
    val activeProfile = sharedConfig.get().activeProfile
    builder.config.useArnRegion = builder.config.useArnRegion ?: S3Setting.UseArnRegion.resolve(provider) ?: activeProfile.useArnRegion
    builder.config.disableMrap = builder.config.disableMrap ?: S3Setting.DisableMultiRegionAccessPoints.resolve(provider) ?: activeProfile.disableMrap
    builder.config.disableS3ExpressSessionAuth = builder.config.disableS3ExpressSessionAuth ?: S3Setting.DisableS3ExpressSessionAuth.resolve(provider) ?: activeProfile.disableS3ExpressSessionAuth
}

private val AwsProfile.useArnRegion: Boolean?
    get() = getBooleanOrNull("s3_use_arn_region")

private val AwsProfile.disableMrap: Boolean?
    get() = getBooleanOrNull("s3_disable_multiregion_access_points")

private val AwsProfile.disableS3ExpressSessionAuth: Boolean?
    get() = getBooleanOrNull("s3_disable_express_session_auth")
