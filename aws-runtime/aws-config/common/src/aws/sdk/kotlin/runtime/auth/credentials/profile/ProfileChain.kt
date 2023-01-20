/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials.profile

import aws.sdk.kotlin.runtime.auth.credentials.ProviderConfigurationException
import aws.sdk.kotlin.runtime.auth.credentials.profile.LeafProviderResult.Err
import aws.sdk.kotlin.runtime.config.profile.AwsProfile
import aws.sdk.kotlin.runtime.config.profile.ProfileMap
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

/**
 * A chain of profile providers
 *
 * Within a profile file, a chain of providers is produced. Starting with a leaf provider,
 * subsequent providers use the credentials form previous providers to perform their task.
 *
 * A chain is a direct representation of the profile file, it can contain [LeafProvider.NamedSource]
 * providers that don't actually have implementations.
 */
internal data class ProfileChain(
    /**
     * The credentials provider that starts the assume role chain.
     */
    val leaf: LeafProvider,

    /**
     * The list of roles to assume (in-order). The first role will be assumed with credentials from [leaf].
     * Every role after that should be assumed with the prior role credentials.
     */
    val roles: List<RoleArn>,
) {
    companion object {
        internal fun resolve(profiles: ProfileMap, profileName: String): ProfileChain {
            val visited = mutableSetOf<String>()
            val chain = mutableListOf<RoleArn>()
            var sourceProfileName = profileName
            var leaf: LeafProvider?

            loop@while (true) {
                val profile = profiles.getOrThrow(sourceProfileName) {
                    if (visited.isEmpty()) {
                        "could not find source profile $sourceProfileName"
                    } else {
                        "could not find source profile $sourceProfileName referenced from ${visited.last()}"
                    }
                }

                if (!visited.add(sourceProfileName)) {
                    // we're in a loop, break out
                    throw ProviderConfigurationException("profile formed an infinite loop: ${visited.joinToString(separator = " -> ")} -> $sourceProfileName")
                }

                // when chaining assume role profiles, SDKs MUST terminate the chain as soon as they hit a profile with static credentials
                if (visited.size > 1) {
                    leaf = profile.staticCredsOrNull()
                    if (leaf != null) break@loop
                }

                // the existence of `role_arn` is the only signal that multiple profiles will be chained
                val roleArn = profile.roleArnOrNull()
                if (roleArn != null) {
                    chain.add(roleArn)
                } else {
                    // have to find a leaf provider or error
                    leaf = profile.leafProvider()
                    break@loop
                }

                // a profile is allowed to use itself as a source profile for static credentials
                when (val nextProfile = profile.chainProvider()) {
                    is NextProfile.SelfReference -> {
                        // self-referential profile, attempt to load as a leaf provider (credential source)
                        leaf = profile.leafProvider()
                        break@loop
                    }
                    is NextProfile.Named -> sourceProfileName = nextProfile.name
                }
            }

            chain.reverse()
            return ProfileChain(checkNotNull(leaf), chain)
        }
    }
}

private inline fun ProfileMap.getOrThrow(name: String, lazyMessage: () -> String): AwsProfile {
    val props = get(name) ?: throw ProviderConfigurationException(lazyMessage())
    return AwsProfile(name, props)
}

/**
 * A profile that specifies a role to assume
 *
 * A RoleArn can only be created from either a profile with `source_profile` or
 * one with `credential_source`
 */
internal data class RoleArn(
    /**
     * ARN of role to assume
     */
    val roleArn: String,
    /**
     * Session name to pass to the assume role provider
     */
    val sessionName: String? = null,

    /**
     * External ID to pass to the assume role provider
     */
    val externalId: String? = null,
)

private const val ROLE_ARN = "role_arn"
private const val EXTERNAL_ID = "external_id"
private const val ROLE_SESSION_NAME = "role_session_name"
private const val CREDENTIAL_SOURCE = "credential_source"
private const val SOURCE_PROFILE = "source_profile"

private const val WEB_IDENTITY_TOKEN_FILE = "web_identity_token_file"
private const val AWS_ACCESS_KEY_ID = "aws_access_key_id"
private const val AWS_SECRET_ACCESS_KEY = "aws_secret_access_key"
private const val AWS_SESSION_TOKEN = "aws_session_token"

private const val SSO_START_URL = "sso_start_url"
private const val SSO_REGION = "sso_region"
private const val SSO_ACCOUNT_ID = "sso_account_id"
private const val SSO_ROLE_NAME = "sso_role_name"

private const val CREDENTIAL_PROCESS = "credential_process"

private fun AwsProfile.roleArnOrNull(): RoleArn? {
    // web identity tokens are leaf providers, not chained roles
    if (contains(WEB_IDENTITY_TOKEN_FILE)) return null

    val roleArn = get(ROLE_ARN) ?: return null

    return RoleArn(
        roleArn,
        sessionName = get(ROLE_SESSION_NAME),
        externalId = get(EXTERNAL_ID),
    )
}

private sealed class LeafProviderResult {
    /**
     * Success, provider found and configured
     */
    data class Ok(val provider: LeafProvider) : LeafProviderResult()

    /**
     * Provider was found but had missing or invalid configuration
     */
    data class Err(val errorMessage: String) : LeafProviderResult()
}

/**
 * Unwrap the result or throw an exception if the result is [Err]
 */
private fun LeafProviderResult.unwrap(): LeafProvider = when (this) {
    is LeafProviderResult.Ok -> provider
    is Err -> throw ProviderConfigurationException(errorMessage)
}

/**
 * Returns the current result if not null or computes it by invoking [fn]
 */
private inline fun LeafProviderResult?.unwrapOrElse(fn: () -> LeafProviderResult): LeafProviderResult = when (this) {
    null -> fn()
    else -> this
}

/**
 * Return current result if not null, otherwise use the result from calling [fn]
 */
private inline fun LeafProviderResult?.orElse(fn: () -> LeafProviderResult?): LeafProviderResult? = when (this) {
    null -> fn()
    else -> this
}

/**
 * Attempt to load [LeafProvider.WebIdentityTokenRole] from the current profile or `null` if the profile
 * does not contain a web identity token provider
 */
private fun AwsProfile.webIdentityTokenCreds(): LeafProviderResult? {
    val roleArn = get(ROLE_ARN)
    val tokenFile = get(WEB_IDENTITY_TOKEN_FILE)
    val sessionName = get(ROLE_SESSION_NAME)
    return when {
        tokenFile == null -> null
        roleArn == null -> LeafProviderResult.Err("profile ($name) missing `$ROLE_ARN`")
        else -> LeafProviderResult.Ok(LeafProvider.WebIdentityTokenRole(roleArn, tokenFile, sessionName))
    }
}

/**
 * Attempt to load [LeafProvider.Sso] from the current profile or `null` if the current profile does not contain
 * an SSO provider
 */
private fun AwsProfile.ssoCreds(): LeafProviderResult? {
    if (!contains(SSO_START_URL) && !contains(SSO_REGION) && !contains(SSO_ACCOUNT_ID) && !contains(SSO_ROLE_NAME)) return null

    // if one or more of the above configuration values is present the profile MUST be resolved by the SSO credential provider.
    val startUrl = get(SSO_START_URL) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_START_URL`")
    val ssoRegion = get(SSO_REGION) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_REGION`")
    val accountId = get(SSO_ACCOUNT_ID) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_ACCOUNT_ID`")
    val roleName = get(SSO_ROLE_NAME) ?: return LeafProviderResult.Err("profile ($name) missing `$SSO_ROLE_NAME`")

    return LeafProviderResult.Ok(LeafProvider.Sso(startUrl, ssoRegion, accountId, roleName))
}

/**
 * Attempt to load [LeafProvider.Process] from the current profile or `null` if the current profile does not contain
 * a credentials process command to execute
 */
private fun AwsProfile.processCreds(): LeafProviderResult? {
    if (!contains(CREDENTIAL_PROCESS)) return null

    val credentialProcess = get(CREDENTIAL_PROCESS) ?: return LeafProviderResult.Err("profile ($name) missing `$CREDENTIAL_PROCESS`")

    return LeafProviderResult.Ok(LeafProvider.Process(credentialProcess))
}

/**
 * Load [LeafProvider.AccessKey] from the current profile or throw an exception if the profile does not contain
 * credentials
 */
private fun AwsProfile.staticCreds(): LeafProviderResult {
    val accessKeyId = get(AWS_ACCESS_KEY_ID)
    val secretKey = get(AWS_SECRET_ACCESS_KEY)
    return when {
        accessKeyId == null && secretKey == null -> LeafProviderResult.Err("profile ($name) did not contain credential information")
        accessKeyId == null -> LeafProviderResult.Err("profile ($name) missing `aws_access_key_id`")
        secretKey == null -> LeafProviderResult.Err("profile ($name) missing `aws_secret_access_key`")
        else -> {
            val sessionToken = get(AWS_SESSION_TOKEN)
            val provider = LeafProvider.AccessKey(Credentials(accessKeyId, secretKey, sessionToken))
            LeafProviderResult.Ok(provider)
        }
    }
}

/**
 * Attempt to load [LeafProvider.AccessKey] from the current profile or `null` if the current profile does not contain
 * credentials
 */
private fun AwsProfile.staticCredsOrNull(): LeafProvider? = when (val result = staticCreds()) {
    is LeafProviderResult.Ok -> result.provider
    else -> null
}

private sealed class NextProfile {
    object SelfReference : NextProfile()
    data class Named(val name: String) : NextProfile()
}

/**
 * Get the next profile name in the chain or the current profile if it specifies an explicit credential source
 */
private fun AwsProfile.chainProvider(): NextProfile {
    val sourceProfile = get(SOURCE_PROFILE)
    val credSource = get(CREDENTIAL_SOURCE)

    return when {
        sourceProfile != null && credSource != null -> throw ProviderConfigurationException("profile ($name) contained both `source_profile` and `credential_source`. Only one or the other can be defined.")
        sourceProfile == null && credSource == null -> throw ProviderConfigurationException("profile ($name) must contain `source_profile` or `credential_source` but neither were defined")
        sourceProfile != null && credSource == null -> if (sourceProfile == name) {
            NextProfile.SelfReference
        } else {
            NextProfile.Named(sourceProfile)
        }
        // loop back into this profile and pick up the credential source
        else -> NextProfile.SelfReference
    }
}

/**
 * Get a terminal leaf provider for the current profile or throw an exception
 */
private fun AwsProfile.leafProvider(): LeafProvider {
    // profile must define either `credential_source` or explicit access keys
    val credSource = get(CREDENTIAL_SOURCE)
    if (credSource != null) return LeafProvider.NamedSource(credSource)

    // we want to stop on errors in earlier providers to get the right exception message, thus we take the first
    // non-null LeafProviderResult we encounter
    return webIdentityTokenCreds()
        .orElse(::ssoCreds)
        .orElse(::processCreds)
        .unwrapOrElse(::staticCreds)
        .unwrap()
}
