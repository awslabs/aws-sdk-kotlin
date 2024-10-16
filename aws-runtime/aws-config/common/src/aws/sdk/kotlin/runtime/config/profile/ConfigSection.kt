/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.InternalSdkApi

@InternalSdkApi
@ExposedCopyVisibility // FIXME Change to @ConsistentCopyVisibility in 1.4.x minor version bump
public data class ConfigSection internal constructor(
    public val name: String,
    internal val properties: Map<String, AwsConfigValue>,
    internal val sectionType: ConfigSectionType = ConfigSectionType.PROFILE,
) {
    public operator fun contains(key: String): Boolean = properties.containsKey(key)
    public fun getOrNull(key: String, subKey: String? = null): String? = properties[key]?.let {
        when (it) {
            is AwsConfigValue.String -> {
                require(subKey == null) { "property '$key' is a string, but caller specified a sub-key" }
                it.value
            }
            is AwsConfigValue.Map -> {
                require(subKey != null) { "property '$key' has sub-properties, caller must specify a sub-key" }
                it[subKey]
            }
        }
    }
}

internal enum class ConfigSectionType {
    PROFILE,
    SSO_SESSION,
    SERVICES,
    UNKNOWN,
}
