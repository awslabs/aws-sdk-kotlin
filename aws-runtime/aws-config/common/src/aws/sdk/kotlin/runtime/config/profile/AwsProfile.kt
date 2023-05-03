/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.config.RetryMode

/**
 * Represents an AWS config profile.
 */
@InternalSdkApi
public typealias AwsProfile = ConfigSection

// Standard cross-SDK properties

/**
 * The AWS signing and endpoint region to use for a profile
 */
@InternalSdkApi
public val AwsProfile.region: String?
    get() = getOrNull("region")

/**
 * The identifier that specifies the entity making the request for a profile
 */
@InternalSdkApi
public val AwsProfile.awsAccessKeyId: String?
    get() = getOrNull("aws_access_key_id")

/**
 * The credentials that authenticate the entity specified by the access key
 */
@InternalSdkApi
public val AwsProfile.awsSecretAccessKey: String?
    get() = getOrNull("aws_secret_access_key")

/**
 * A semi-temporary session token that authenticates the entity is allowed to access a specific set of resources
 */
@InternalSdkApi
public val AwsProfile.awsSessionToken: String?
    get() = getOrNull("aws_session_token")

/**
 * A role that the user must automatically assume, giving it semi-temporary access to a specific set of resources
 */
@InternalSdkApi
public val AwsProfile.roleArn: String?
    get() = getOrNull("role_arn")

/**
 * Specifies which profile must be used to automatically assume the role specified by role_arn
 */
@InternalSdkApi
public val AwsProfile.sourceProfile: String?
    get() = getOrNull("source_profile")

/**
 * The maximum number of request attempts to perform. This is one more than the number of retries, so
 * aws.maxAttempts = 1 will have 0 retries.
 */
@InternalSdkApi
public val AwsProfile.maxAttempts: Int?
    get() = getOrNull("max_attempts")?.run {
        toIntOrNull() ?: throw ConfigurationException("Failed to parse maxAttempts $this as an integer")
    }

/**
 * The command which the SDK will invoke to retrieve credentials
 */
@InternalSdkApi
public val AwsProfile.credentialProcess: String?
    get() = getOrNull("credential_process")

/**
 * Which [RetryMode] to use for the default RetryPolicy, when one is not specified at the client level.
 */
@InternalSdkApi
public val AwsProfile.retryMode: RetryMode?
    get() = getOrNull("retry_mode")?.run {
        RetryMode.values().firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: throw ConfigurationException("Retry mode $this is not supported, should be one of: ${RetryMode.values().joinToString(", ")}")
    }

/**
 * Whether service clients should make requests to the FIPS endpoint variant.
 */
@InternalSdkApi
public val AwsProfile.useFips: Boolean?
    get() = getBooleanOrNull("use_fips_endpoint")

/**
 * Whether service clients should make requests to the dual-stack endpoint variant.
 */
@InternalSdkApi
public val AwsProfile.useDualStack: Boolean?
    get() = getBooleanOrNull("use_dualstack_endpoint")

/**
 * Parse a config value as a boolean, ignoring case.
 */
@InternalSdkApi
public fun AwsProfile.getBooleanOrNull(key: String, subKey: String? = null): Boolean? =
    getOrNull(key, subKey)?.let {
        it.lowercase().toBooleanStrictOrNull() ?: throw ConfigurationException(
            buildString {
                append("Failed to parse config property $key")
                subKey?.let { append(".$it") }
                append(" as a boolean")
            },
        )
    }
