/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.AwsUserAgentMetadata
import aws.sdk.kotlin.runtime.http.operation.CustomUserAgentMetadata
import aws.smithy.kotlin.runtime.http.operation.ModifyRequestMiddleware
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.io.middleware.Phase

internal const val X_AMZ_USER_AGENT: String = "x-amz-user-agent"
internal const val USER_AGENT: String = "User-Agent"

/**
 *  Http middleware that sets the User-Agent and x-amz-user-agent headers
 */
@InternalSdkApi
public class UserAgent(
    /**
     * Metadata that doesn't change per/request (e.g. sdk and environment related metadata)
     */
    private val staticMetadata: AwsUserAgentMetadata,
) : ModifyRequestMiddleware {

    override fun install(op: SdkHttpOperation<*, *>) {
        op.execution.mutate.register(this, Phase.Order.After)
    }

    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        // pull dynamic values out of the context
        val customMetadata = req.context.getOrNull(CustomUserAgentMetadata.ContextKey)

        // resolve the metadata for the request which is a combination of the static and per/operation metadata
        val requestMetadata = when {
            customMetadata == null -> staticMetadata
            staticMetadata.customMetadata == null -> staticMetadata.copy(customMetadata = customMetadata)
            else -> staticMetadata.copy(customMetadata = staticMetadata.customMetadata + customMetadata)
        }

        // NOTE: Due to legacy issues with processing the user agent, the original content for
        // x-amz-user-agent and User-Agent is swapped here.  See top note in the
        // sdk-user-agent-header SEP and https://github.com/smithy-lang/smithy-kotlin/issues/373
        // for further details.
        req.subject.headers[USER_AGENT] = requestMetadata.xAmzUserAgent
        req.subject.headers[X_AMZ_USER_AGENT] = requestMetadata.userAgent

        return req
    }
}
