/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.operation.ConfigMetadata
import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.sdk.kotlin.runtime.http.operation.FeatureMetadata
import aws.smithy.kotlin.runtime.util.*
import kotlin.jvm.JvmInline

internal const val AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV"
internal const val AWS_APP_ID_ENV = "AWS_SDK_UA_APP_ID"

// non-standard environment variables/properties
internal const val AWS_APP_ID_PROP = "aws.userAgentAppId"
internal const val FRAMEWORK_METADATA_ENV = "AWS_FRAMEWORK_METADATA"
internal const val FRAMEWORK_METADATA_PROP = "aws.frameworkMetadata"

/**
 * Metadata used to populate the `User-Agent` and `x-amz-user-agent` headers
 */
public data class AwsUserAgentMetadata(
    val sdkMetadata: SdkMetadata,
    val apiMetadata: ApiMetadata,
    val osMetadata: OsMetadata,
    val languageMetadata: LanguageMetadata,
    val execEnvMetadata: ExecutionEnvMetadata? = null,
    val frameworkMetadata: FrameworkMetadata? = null,
    val appId: String? = null,
    val customMetadata: CustomUserAgentMetadata? = null
) {

    public companion object {
        /**
         * Load user agent configuration data from the current environment
         */
        public fun fromEnvironment(
            apiMeta: ApiMetadata,
        ): AwsUserAgentMetadata = loadAwsUserAgentMetadataFromEnvironment(Platform, apiMeta)
    }

    /**
     * New-style user agent header value for `x-amz-user-agent`
     */
    val xAmzUserAgent: String
        get() {
            /*
               ABNF for the user agent:
               ua-string =
                   [internal-metadata RWS]
                   sdk-metadata RWS
                   [api-metadata RWS]
                   os-metadata RWS
                   language-metadata RWS
                   [env-metadata RWS]
                   *(feat-metadata RWS)
                   *(config-metadata RWS)
                   *(framework-metadata RWS)
                   [appId]
             */
            val ua = mutableListOf<String>()

            val isInternal = customMetadata?.extras?.remove("internal")
            if (isInternal != null) {
                ua.add("md/internal")
            }

            ua.add("$sdkMetadata")
            ua.add("$apiMetadata")
            ua.add("$osMetadata")
            ua.add("$languageMetadata")
            execEnvMetadata?.let { ua.add("$it") }

            val features = customMetadata?.typedExtras?.filterIsInstance<FeatureMetadata>()
            features?.forEach { ua.add("$it") }

            val config = customMetadata?.typedExtras?.filterIsInstance<ConfigMetadata>()
            config?.forEach { ua.add("$it") }

            frameworkMetadata?.let { ua.add("$it") }
            appId?.let { ua.add("app/$it") }

            customMetadata?.extras?.let {
                val wrapper = AdditionalMetadata(it)
                ua.add("$wrapper")
            }

            return ua.joinToString(separator = " ")
        }

    /**
     * Legacy user agent header value for `UserAgent`
     */
    val userAgent: String
        get() = "$sdkMetadata"
}

internal fun loadAwsUserAgentMetadataFromEnvironment(platform: PlatformProvider, apiMeta: ApiMetadata): AwsUserAgentMetadata {
    val sdkMeta = SdkMetadata("kotlin", apiMeta.version)
    val osInfo = platform.osInfo()
    val osMetadata = OsMetadata(osInfo.family, osInfo.version)
    val langMeta = platformLanguageMetadata()
    val appId = platform.getProperty(AWS_APP_ID_PROP) ?: platform.getenv(AWS_APP_ID_ENV)

    val frameworkMetadata = FrameworkMetadata.fromEnvironment(platform)
    return AwsUserAgentMetadata(
        sdkMeta,
        apiMeta,
        osMetadata,
        langMeta,
        detectExecEnv(platform),
        frameworkMetadata = frameworkMetadata,
        appId = appId,
    )
}

/**
 * Wrapper around additional metadata kv-pairs that handles formatting
 */
@JvmInline
internal value class AdditionalMetadata(private val extras: Map<String, String>) {
    override fun toString(): String = extras.entries.joinToString(separator = " ") { entry ->
        when (entry.value.lowercase()) {
            "true" -> "md/${entry.key}"
            else -> "md/${entry.key.encodeUaToken()}/${entry.value.encodeUaToken()}"
        }
    }
}

/**
 * SDK metadata
 * @property name The SDK (language) name
 * @property version The SDK version
 */
@InternalSdkApi
public data class SdkMetadata(val name: String, val version: String) {
    override fun toString(): String = "aws-sdk-$name/$version"
}

/**
 * API metadata
 * @property serviceId The service ID (sdkId) in use (e.g. "Api Gateway")
 * @property version The version of the client (note this may be the same as [SdkMetadata.version] for SDK's
 * that don't independently version clients from one another.
 */
@InternalSdkApi
public data class ApiMetadata(val serviceId: String, val version: String) {
    override fun toString(): String {
        val formattedServiceId = serviceId.replace(" ", "-").lowercase()
        return "api/$formattedServiceId/${version.encodeUaToken()}"
    }
}

/**
 * Operating system metadata
 */
@InternalSdkApi
public data class OsMetadata(val family: OsFamily, val version: String? = null) {
    override fun toString(): String {
        // os-family = windows / linux / macos / android / ios / other
        val familyStr = when (family) {
            OsFamily.Unknown -> "other"
            else -> family.toString()
        }
        return if (version != null) "os/$familyStr/${version.encodeUaToken()}" else "os/$familyStr"
    }
}

/**
 * Programming language metadata
 * @property version The kotlin version in use
 * @property extras Additional key value pairs appropriate for the language/runtime (e.g.`jvmVm=OpenJdk`, etc)
 */
@InternalSdkApi
public data class LanguageMetadata(
    val version: String = KotlinVersion.CURRENT.toString(),
    // additional metadata key/value pairs
    val extras: Map<String, String> = emptyMap()
) {
    override fun toString(): String = buildString {
        append("lang/kotlin/$version")
        if (extras.isNotEmpty()) {
            val wrapper = AdditionalMetadata(extras)
            append(" $wrapper")
        }
    }
}

// provide platform specific metadata
internal expect fun platformLanguageMetadata(): LanguageMetadata

/**
 * Execution environment metadata
 * @property name The execution environment name (e.g. "lambda")
 */
@InternalSdkApi
public data class ExecutionEnvMetadata(val name: String) {
    override fun toString(): String = "exec-env/${name.encodeUaToken()}"
}

/**
 * Framework metadata (e.g. name = "amplify" version = "1.2.3")
 * @property name The framework name
 * @property version The framework version
 */
@InternalSdkApi
public data class FrameworkMetadata(
    val name: String,
    val version: String,
) {
    internal companion object {
        internal fun fromEnvironment(provider: PlatformEnvironProvider): FrameworkMetadata? {
            val kvPair = provider.getProperty(FRAMEWORK_METADATA_PROP) ?: provider.getenv(FRAMEWORK_METADATA_ENV)
            return kvPair?.let {
                val kv = kvPair.split(':', limit = 2)
                check(kv.size == 2) { "Invalid value for FRAMEWORK_METADATA: $kvPair; must be of the form `name:version`" }
                FrameworkMetadata(kv[0], kv[1])
            }
        }
    }

    override fun toString(): String = "lib/$name/$version"
}

private fun detectExecEnv(platform: PlatformEnvironProvider): ExecutionEnvMetadata? =
    platform.getenv(AWS_EXECUTION_ENV)?.let {
        ExecutionEnvMetadata(it)
    }

// ua-value = token
// token = 1*tchar
// tchar = "!" / "#" / "$" / "%" / "&" / "'" / "*" / "+" / "-" / "." /
//         "^" / "_" / "`" / "|" / "~" / DIGIT / ALPHA
private val VALID_TCHAR = setOf(
    '!', '#', '$', '%', '&',
    '\'', '*', '+', '-', '.',
    '^', '_', '`', '|', '~'
)

private fun String.encodeUaToken(): String {
    val str = this
    return buildString(str.length) {
        for (chr in str) {
            when (chr) {
                ' ' -> append("_")
                in 'a'..'z', in 'A'..'Z', in '0'..'9', in VALID_TCHAR -> append(chr)
                else -> continue
            }
        }
    }
}
