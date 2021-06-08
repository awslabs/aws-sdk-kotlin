/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.AwsUserAgentMetadata
import software.aws.clientrt.http.Feature
import software.aws.clientrt.http.FeatureKey
import software.aws.clientrt.http.HttpClientFeatureFactory
import software.aws.clientrt.http.operation.SdkHttpOperation

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
            // NOTE: Due to legacy issues with processing the user agent, the original content for
            // x-amz-user-agent and User-Agent is swapped here.  See top note in the
            // sdk-user-agent-header SEP for further details.
            req.subject.headers[USER_AGENT] = awsUserAgentMetadata.xAmzUserAgent
            req.subject.headers[X_AMZ_USER_AGENT] = awsUserAgentMetadata.userAgent
            next.call(req)
        }
    }
}
