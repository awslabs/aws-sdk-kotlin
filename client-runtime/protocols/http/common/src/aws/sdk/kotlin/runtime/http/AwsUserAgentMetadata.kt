/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import software.aws.clientrt.util.OsFamily
import software.aws.clientrt.util.Platform

/**
 * Metadata used to populate the `User-Agent` and `x-amz-user-agent` headers
 */
public data class AwsUserAgentMetadata(
    val sdkMetadata: SdkMetadata,
    val apiMetadata: ApiMetadata,
    val osMetadata: OsMetadata,
    val languageMetadata: LanguageMetadata,
    val execEnvMetadata: ExecutionEnvMetadata? = null
) {

    public companion object {
        /**
         * Load user agent configuration data from the current environment
         */
        public fun fromEnvironment(apiMeta: ApiMetadata): AwsUserAgentMetadata {
            val sdkMeta = SdkMetadata("kotlin", apiMeta.version)
            val osInfo = Platform.osInfo()
            val osMetadata = OsMetadata(osInfo.family, osInfo.version)
            val langMeta = platformLanguageMetadata()
            return AwsUserAgentMetadata(sdkMeta, apiMeta, osMetadata, langMeta, detectExecEnv())
        }
    }

    /**
     * New-style user agent header value for `x-amz-user-agent`
     */
    val xAmzUserAgent: String = buildString {
        /*
           ABNF for the user agent:
           ua-string =
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
        append("$sdkMetadata ")
        append("$apiMetadata ")
        append("$osMetadata ")
        append("$languageMetadata ")
        execEnvMetadata?.let { append("$it") }

        // TODO - feature metadata
        // TODO - config metadata
        // TODO - framework metadata (e.g. Amplify would be a good candidate for this data)
        // TODO - appId
    }.trimEnd()

    /**
     * Legacy user agent header value for `UserAgent`
     */
    val userAgent: String = "$sdkMetadata"
}

/**
 * SDK metadata
 * @property name The SDK (language) name
 * @property version The SDK version
 */
public data class SdkMetadata(val name: String, val version: String) {
    override fun toString(): String = "aws-sdk-$name/$version"
}

/**
 * API metadata
 * @property serviceId The service ID (sdkId) in use (e.g. "Api Gateway")
 * @property version The version of the client (note this may be the same as [SdkMetadata.version] for SDK's
 * that don't independently version clients from one another.
 */
public data class ApiMetadata(val serviceId: String, val version: String) {
    override fun toString(): String {
        val formattedServiceId = serviceId.replace(" ", "-").toLowerCase()
        return "api/$formattedServiceId/${version.encodeUaToken()}"
    }
}

/**
 * Operating system metadata
 */
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
public data class LanguageMetadata(
    val version: String = KotlinVersion.CURRENT.toString(),
    // additional metadata key/value pairs
    val extras: Map<String, String> = emptyMap()
) {
    override fun toString(): String = buildString {
        append("lang/kotlin/$version")
        extras.entries.forEach { (key, value) ->
            append(" md/$key/${value.encodeUaToken()}")
        }
    }
}

// provide platform specific metadata
internal expect fun platformLanguageMetadata(): LanguageMetadata

/**
 * Execution environment metadata
 * @property name The execution environment name (e.g. "lambda")
 */
public data class ExecutionEnvMetadata(val name: String) {
    override fun toString(): String = "exec-env/${name.encodeUaToken()}"
}

private fun detectExecEnv(): ExecutionEnvMetadata? {
    // see https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html#configuration-envvars-runtime
    return Platform.getenv("AWS_LAMBDA_FUNCTION_NAME")?.let {
        ExecutionEnvMetadata("lambda")
    }
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
