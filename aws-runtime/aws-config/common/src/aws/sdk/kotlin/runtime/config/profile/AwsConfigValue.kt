/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Container for the different types of property values within an AWS config profile.
 * A property can either be a string or mapping of strings. Maps cannot be nested.
 */
@InternalSdkApi
public sealed class AwsConfigValue {
    public data class String(public val value: kotlin.String) : AwsConfigValue() {
        override fun toString(): kotlin.String = value
    }

    public data class Map(public val value: kotlin.collections.Map<kotlin.String, kotlin.String>) :
        AwsConfigValue(), kotlin.collections.Map<kotlin.String, kotlin.String> by value {
        override fun toString(): kotlin.String = value.toString()
    }
}

@InternalSdkApi
public fun AwsConfigValue.asStringOrNull(): String? = (this as? AwsConfigValue.String)?.value

@InternalSdkApi
public fun AwsConfigValue.asMapOrNull(): Map<String, String>? = this as? AwsConfigValue.Map
