/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.useragent

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.sdkUserAgentAppId
import aws.sdk.kotlin.runtime.http.AWS_APP_ID_ENV
import aws.sdk.kotlin.runtime.http.AWS_APP_ID_PROP
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve user agent from specified sources. Returns null if none found
 */
@InternalSdkApi
public suspend fun resolveUserAgent(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): String? =
    platform.getProperty(AWS_APP_ID_PROP) ?: platform.getenv(AWS_APP_ID_ENV) ?: profile.get().sdkUserAgentAppId
