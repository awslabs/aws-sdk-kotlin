/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.resolve
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.PlatformProvider

/**
 * Load the properties of the specified or default AWS configuration profile.  This
 * function will return the properties of the profile specified by the local environment
 * or the default profile if none is defined.
 *
 * This function performs no caching. File I/O will be performed with each call.
 *
 * @param platform used for unit testing
 *
 * @return an [AwsProfile] regardless if local configuration files are available
 */
@InternalSdkApi
public suspend fun loadActiveAwsProfile(platform: PlatformProvider): AwsProfile {
    // Determine active profile and location of configuration files
    val source = resolveConfigSource(platform)

    // Read all profiles from local system
    val allProfiles = loadAwsProfiles(platform, source)

    // Return the active profile
    return AwsProfile(source.profile, allProfiles[source.profile] ?: emptyMap())
}

/**
 * Load all profiles specified in local configuration files.
 *
 * @param platform Platform from which to resolve configuration data
 * @param source Specifies the location of the configuration files
 *
 * @return A map of all profiles, which each are a map of key/value pairs.
 */
private suspend fun loadAwsProfiles(platform: PlatformProvider, source: AwsConfigurationSource): Map<String, Map<String, String>> {

    // merged AWS configuration based on optional configuration and credential file contents
    return mergeProfiles(
        parse(FileType.CONFIGURATION, platform.readFileOrNull(source.configPath)?.decodeToString()),
        parse(FileType.CREDENTIAL, platform.readFileOrNull(source.credentialsPath)?.decodeToString()),
    )
}

// Merge contents of profile maps
internal fun mergeProfiles(vararg maps: ProfileMap) = buildMap<String, Map<String, String>> {
    maps.forEach { map ->
        map.entries.forEach { entry ->
            put(entry.key, (get(entry.key) ?: emptyMap()) + entry.value)
        }
    }
}

// Specifies the active profile and configured (may not actually exist) locations of configuration files.
internal data class AwsConfigurationSource(val profile: String, val configPath: String, val credentialsPath: String)

/**
 * Determine the source of AWS configuration
 */
internal fun resolveConfigSource(platform: PlatformProvider) =
    AwsConfigurationSource(
        // If the user does not specify the profile to be used, the default profile must be used by the SDK.
        // The default profile must be overridable using the AWS_PROFILE environment variable.
        AwsSdkSetting.AwsProfile.resolve(platform) ?: Literals.DEFAULT_PROFILE,
        normalizePath(FileType.CONFIGURATION.path(platform), platform),
        normalizePath(FileType.CREDENTIAL.path(platform), platform)
    )

/**
 * Expands paths prefixed with '~' to the home directory under which the SDK is running.
 *
 * User Home Resolution: The user's home directory must be resolved when the file location starts with ~/ or ~
 * followed by the operating system's default path separator by checking the following variables, in order:
 *
 * 1. (All Platforms) The HOME environment variable.
 * 2. (Windows Platforms) The USERPROFILE environment variable.
 * 3. (Windows Platforms) The HOMEDRIVE environment variable prepended to the HOMEPATH environment variable (ie. $HOMEDRIVE$HOMEPATH).
 * 4. (Optional) A language-specific home path resolution function or variable.
 */
internal fun normalizePath(path: String, platform: PlatformProvider): String {
    if (!path.trim().startsWith('~')) return path

    val home = resolveHomeDir(platform) ?: error("Unable to determine user home directory")

    return home + path.substring(1)
}

/**
 * Load the user's home directory based on the priorities:
 *
 * If the implementation cannot determine the customer's platform, the USERPROFILE and HOMEDRIVE + HOMEPATH environment
 * variables must be checked for all platforms. If the implementation can determine the customer's platform, the
 * USERPROFILE and HOMEDRIVE + HOMEPATH environment variables must not be checked on non-windows platforms.
 *
 * @param
 * @return the absolute path of the home directory from which the SDK is running, or null if unspecified by environment.
 */
private fun resolveHomeDir(platform: PlatformProvider): String? =
    with(platform) {
        when (osInfo().family) {
            OsFamily.Unknown,
            OsFamily.Windows ->
                getenv("HOME")
                    ?: getenv("USERPROFILE")
                    ?: (getenv("HOMEDRIVE") to getenv("HOMEPATH")).concatOrNull()
                    ?: getProperty("user.home")
            else ->
                getenv("HOME")
                    ?: getProperty("user.home")
        }
    }
