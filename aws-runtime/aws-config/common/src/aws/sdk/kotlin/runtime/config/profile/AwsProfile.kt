/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.ConfigurationException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.endpoints.AccountIdEndpointMode
import aws.smithy.kotlin.runtime.client.config.RetryMode
import aws.smithy.kotlin.runtime.net.url.Url

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
    get() = getIntOrNull("max_attempts")

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
            ?: throw ConfigurationException(
                "retry_mode $this is not supported, should be one of: ${
                    RetryMode.values().joinToString(", ") { it.name.lowercase() }
                }",
            )
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
 * The default endpoint URL that applies to all services.
 */
@InternalSdkApi
public val AwsProfile.endpointUrl: Url?
    get() = getUrlOrNull("endpoint_url")

/**
 * Whether to ignore configured endpoint URLs.
 */
@InternalSdkApi
public val AwsProfile.ignoreEndpointUrls: Boolean?
    get() = getBooleanOrNull("ignore_configured_endpoint_urls")

/**
 * The name of the services config section used by this profile.
 */
@InternalSdkApi
public val AwsProfile.servicesSection: String?
    get() = getOrNull("services")

/**
 * The SDK user agent app ID used to identify applications.
 */
@InternalSdkApi
public val AwsProfile.sdkUserAgentAppId: String?
    get() = getOrNull("sdk_ua_app_id")

/**
 * Whether service clients should make requests to the dual-stack endpoint variant.
 */
@InternalSdkApi
public val AwsProfile.accountIdEndpointMode: AccountIdEndpointMode?
    get() = getOrNull("account_id_endpoint_mode")?.run {
        AccountIdEndpointMode.values().firstOrNull { it.name.equals(this, ignoreCase = true) }
            ?: throw ConfigurationException(
                "account_id_endpoint_mode $this is not supported, should be one of: ${
                    AccountIdEndpointMode.values().joinToString(", ") { it.name.lowercase() }
                }",
            )
    }

/**
 * Determines when a request should be compressed or not
 */
@InternalSdkApi
public val AwsProfile.disableRequestCompression: Boolean?
    get() = getBooleanOrNull("disable_request_compression")

/**
 * The threshold used to determine when a request should be compressed
 */
@InternalSdkApi
public val AwsProfile.requestMinCompressionSizeBytes: Long?
    get() = getLongOrNull("request_min_compression_size_bytes")

/**
 * The set of regions to use when signing a request with sigV4a.
 */
@InternalSdkApi
public val AwsProfile.sigV4aSigningRegionSet: String?
    get() = getOrNull("sigv4a_signing_region_set")

/**
 * todo
 */
@InternalSdkApi
public val AwsProfile.requestChecksumCalculation: String?
    get() = getOrNull("request_checksum_calculation")

/**
 * Parse a config value as a boolean, ignoring case.
 */
@InternalSdkApi
public fun AwsProfile.getBooleanOrNull(key: String, subKey: String? = null): Boolean? =
    getOrNull(key, subKey)?.let {
        it.lowercase().toBooleanStrictOrNull() ?: throw ConfigurationException(
            "Failed to parse config property ${buildKeyString(key, subKey)} as a boolean",
        )
    }

/**
 * Parse a config value as an int.
 */
@InternalSdkApi
public fun AwsProfile.getIntOrNull(key: String, subKey: String? = null): Int? =
    getOrNull(key, subKey)?.let {
        it.toIntOrNull() ?: throw ConfigurationException(
            "Failed to parse config property ${buildKeyString(key, subKey)} as an integer",
        )
    }

/**
 * Parse a config value as a long.
 */
@InternalSdkApi
public fun AwsProfile.getLongOrNull(key: String, subKey: String? = null): Long? =
    getOrNull(key, subKey)?.let {
        it.toLongOrNull() ?: throw ConfigurationException(
            "Failed to parse config property ${buildKeyString(key, subKey)} as a long",
        )
    }

internal fun AwsProfile.getUrlOrNull(key: String, subKey: String? = null): Url? =
    getOrNull(key, subKey)?.let {
        try {
            Url.parse(it)
        } catch (e: Exception) {
            throw ConfigurationException("Failed to parse config property ${buildKeyString(key, subKey)} as a URL", e)
        }
    }

private fun buildKeyString(key: String, subKey: String? = null): String =
    listOfNotNull(key, subKey).joinToString(".")
