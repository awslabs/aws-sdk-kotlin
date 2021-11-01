/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.AwsUserAgentMetadata
import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.smithy.kotlin.runtime.http.Feature
import aws.smithy.kotlin.runtime.http.FeatureKey
import aws.smithy.kotlin.runtime.http.HttpClientFeatureFactory
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.io.middleware.Phase

internal const val X_AMZ_USER_AGENT: String = "x-amz-user-agent"
internal const val USER_AGENT: String = "User-Agent"

/**
 *  Http middleware that sets the User-Agent and x-amz-user-agent headers
 */
@InternalSdkApi
public class UserAgent(
    private val staticMetadata: AwsUserAgentMetadata
) : Feature {

    public class Config {
        /**
         * Metadata that doesn't change per/request (e.g. sdk and environment related metadata)
         */
        public var staticMetadata: AwsUserAgentMetadata? = null
    }

    public companion object Feature :
        HttpClientFeatureFactory<Config, UserAgent> {
        override val key: FeatureKey<UserAgent> = FeatureKey("UserAgent")

        override fun create(block: Config.() -> Unit): UserAgent {
            val config = Config().apply(block)
            val metadata = requireNotNull(config.staticMetadata) { "staticMetadata is required" }
            return UserAgent(metadata)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.mutate.intercept(Phase.Order.After) { req, next ->

            // pull dynamic values out of the context
            val customMetadata = req.context.getOrNull(CustomUserAgentMetadata.ContextKey)

            // resolve the metadata for the request which is a combination of the static and per/operation metadata
            val requestMetadata = staticMetadata.copy(customMetadata = customMetadata)

            // NOTE: Due to legacy issues with processing the user agent, the original content for
            // x-amz-user-agent and User-Agent is swapped here.  See top note in the
            // sdk-user-agent-header SEP and https://github.com/awslabs/smithy-kotlin/issues/373
            // for further details.
            req.subject.headers[USER_AGENT] = requestMetadata.xAmzUserAgent
            req.subject.headers[X_AMZ_USER_AGENT] = requestMetadata.userAgent
            next.call(req)
        }
    }
}
