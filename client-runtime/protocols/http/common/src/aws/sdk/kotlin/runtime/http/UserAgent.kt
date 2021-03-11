/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.InternalSdkApi
import software.aws.clientrt.http.Feature
import software.aws.clientrt.http.FeatureKey
import software.aws.clientrt.http.HttpClientFeatureFactory
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.util.OsFamily
import software.aws.clientrt.util.Platform

internal const val X_AMZ_USER_AGENT: String = "x-amz-user-agent"
internal const val USER_AGENT: String = "User-Agent"

/**
 *  Http middleware that sets the User-Agent and x-amz-user-agent headers
 */
@InternalSdkApi
public class UserAgent(private val awsUserAgentMetadata: AwsUserAgentMetadata) : Feature {

    public class Config {
        public var metadata: AwsUserAgentMetadata? = null
    }

    public companion object Feature :
        HttpClientFeatureFactory<Config, UserAgent> {
        override val key: FeatureKey<UserAgent> = FeatureKey("UserAgent")

        override fun create(block: Config.() -> Unit): UserAgent {
            val config = Config().apply(block)
            val metadata = requireNotNull(config.metadata) { "metadata is required" }
            return UserAgent(metadata)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.mutate.intercept { req, next ->
            req.builder.headers[USER_AGENT] = awsUserAgentMetadata.userAgent
            req.builder.headers[X_AMZ_USER_AGENT] = awsUserAgentMetadata.xAmzUserAgent
            next.call(req)
        }
    }
}

public data class SdkMetadata(val name: String, val version: String) {
    override fun toString(): String = "aws-sdk-$name/$version"
}

private fun String.formatServiceIdForUA(): String = replace(" ", "-").toLowerCase()

public data class ApiMetadata(val serviceId: String, val version: String) {
    override fun toString(): String = "api/${serviceId.formatServiceIdForUA()}/$version"
}

public data class OsMetadata(val family: OsFamily, val version: String? = null) {
    override fun toString(): String = if (version != null) "os/$family/${version.encodeUaValue()}" else "os/$family"
}

public data class LanguageMetadata(
    val version: String = KotlinVersion.CURRENT.toString(),
    // additional metadata key/value pairs
    val extras: Map<String, String> = emptyMap()
) {
    override fun toString(): String = buildString {
        append("lang/kotlin/$version")
        extras.entries.forEach { (key, value) ->
            append(" md/$key/${value.encodeUaValue()}")
        }
    }
}

// provide platform specific metadata
internal expect fun platformLanguageMetadata(): LanguageMetadata

public data class ExecutionEnvMetadata(val name: String) {
    override fun toString(): String = "exec-env/$name"
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

private fun String.encodeUaValue(): String {
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
