/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.sigv4a

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.sigv4aSigningRegionSet
import aws.smithy.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.LazyAsyncValue
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Attempts to resolve sigv4aSigningRegionSet from specified sources.
 * @return The sigv4aSigningRegionSet if found, null if not
 */
@InternalSdkApi
public suspend fun resolveSigningRegionSet(platform: PlatformProvider = PlatformProvider.System, profile: LazyAsyncValue<AwsProfile>): List<String>? {
    val rawString = AwsSdkSetting.AwsSigv4aSigningRegionSet.resolve(platform) ?: profile.get().sigv4aSigningRegionSet
    return rawString?.let {
        check(isValidListFormat(it)) { "Config setting sigv4aSigningRegionSet is not formatted as a list of strings" }
        it.split(",")
    }
}

/**
 * Makes sure that a string can be parsed into a list of non-empty strings
 */
internal fun isValidListFormat(input: String): Boolean =
    Regex("^\\s*([^,\\s]+\\s*,\\s*)*[^,\\s]+\\s*\$").matches(input)
