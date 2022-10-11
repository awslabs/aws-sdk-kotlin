/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.config.retries.RetryMode

/**
 * The properties and name of an AWS configuration profile.
 *
 * @property name name of profile
 * @property properties key/value pairs of properties specified by the active profile, accessible via [Map<K, V>]
 */
public data class AwsProfile(
    val name: String,
    private val properties: Map<String, String>,
) : Map<String, String> by properties

// Standard cross-SDK properties

/**
 * The AWS signing and endpoint region to use for a profile
 */
public val AwsProfile.region: String?
    get() = this["region"]

/**
 * The identifier that specifies the entity making the request for a profile
 */
public val AwsProfile.awsAccessKeyId: String?
    get() = this["aws_access_key_id"]

/**
 * The credentials that authenticate the entity specified by the access key
 */
public val AwsProfile.awsSecretAccessKey: String?
    get() = this["aws_secret_access_key"]

/**
 * A semi-temporary session token that authenticates the entity is allowed to access a specific set of resources
 */
public val AwsProfile.awsSessionToken: String?
    get() = this["aws_session_token"]

/**
 * A role that the user must automatically assume, giving it semi-temporary access to a specific set of resources
 */
public val AwsProfile.roleArn: String?
    get() = this["role_arn"]

/**
 * Specifies which profile must be used to automatically assume the role specified by role_arn
 */
public val AwsProfile.sourceProfile: String?
    get() = this["source_profile"]

/**
 * The maximum number of request attempts to perform. This is one more than the number of retries, so
 * aws.maxAttempts = 1 will have 0 retries.
 */
public val AwsProfile.maxAttempts: Int?
    get() = this["max_attempts"]?.run {
        toIntOrNull() ?: throw ConfigurationException("Failed to parse maxAttempts $this as an integer")
    }

/**
 * The external command which the SDK will run to generate or retrieve authentication credentials to use.
 */
public val AwsProfile.credentialProcess: String?
    get() = this["credential_process"]

/**
 * Which [RetryMode] to use for the default RetryPolicy, when one is not specified at the client level.
 */
public val AwsProfile.retryMode: RetryMode?
    get() = this["retry_mode"]?.run {
        RetryMode.values().firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: throw ConfigurationException("Retry mode $this is not supported, should be one of: ${RetryMode.values().joinToString(", ")}")
    }
