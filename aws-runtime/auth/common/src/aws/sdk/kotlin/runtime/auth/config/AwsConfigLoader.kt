package aws.sdk.kotlin.runtime.auth.config

import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.Platform

/**
 * Load the properties of the specified or default AWS configuration profile.  This
 * function will return the properties of the profile specified by the local environment
 * or the default profile if none is defined.
 *
 * @return an [AwsConfiguration] regardless if local configuration files are available
 */
internal fun loadAwsConfiguration(): AwsConfiguration {
    // Determine active profile and location of configuration files
    val source = resolveConfigSource(Platform)

    // merged AWS configuration based on optional configuration and credential file contents
    val allProfiles = mergeProfiles(
        parse(FileType.CONFIGURATION, (Platform::readFile)(source.configPath)),
        parse(FileType.CREDENTIAL, (Platform::readFile)(source.credentialsPath)),
    )

    return AwsConfiguration(source.profile, allProfiles[source.profile] ?: emptyMap())
}

/**
 * The properties and name of the active AWS configuration profile.
 *
 * @property profileName active profile
 * @property properties key/value pairs of properties specified by the active profile, accessible via [Map<K, V>]
 */
data class AwsConfiguration(val profileName: String, private val properties: Map<String, String>) : Map<String, String> by properties

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
internal fun resolveConfigSource(platform: Platform) =
    AwsConfigurationSource(
        envProfileResolver(platform::getenv),
        normalizePath(FileType.CONFIGURATION.resolveFileLocation(platform::getenv, platform.filePathSegment), platform),
        normalizePath(FileType.CREDENTIAL.resolveFileLocation(platform::getenv, platform.filePathSegment), platform)
    )

// If the user does not specify the profile to be used, the default profile must be used by the SDK.
// The default profile must be overridable using the AWS_PROFILE environment variable.
internal fun envProfileResolver(getEnv: (String) -> String?): String = getEnv("AWS_PROFILE") ?: Literals.DEFAULT_PROFILE

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
internal fun normalizePath(path: String, platform: Platform): String {
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
private fun resolveHomeDir(platform: Platform): String? =
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
