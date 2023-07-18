/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.header
import aws.smithy.kotlin.runtime.io.Handler

/**
 * The per/operation unique client side ID header name. This will match
 * the [HttpOperationContext.SdkInvocationId]
 */
internal const val AMZ_SDK_INVOCATION_ID_HEADER = "amz-sdk-invocation-id"

/**
 * Details about the current request such as the attempt number, maximum possible attempts, ttl, etc
 */
internal const val AMZ_SDK_REQUEST_HEADER = "amz-sdk-request"

/**
 * This middleware adds AWS specific retry headers
 */
@InternalSdkApi
public class AwsRetryHeaderMiddleware<O> : MutateMiddleware<O> {
    private var attempt = 0
    private var maxAttempts: Int? = null
    override fun install(op: SdkHttpOperation<*, O>) {
        maxAttempts = op.execution.retryStrategy.config.maxAttempts
        op.execution.onEachAttempt.register(this)
    }

    override suspend fun <H : Handler<SdkHttpRequest, O>> handle(request: SdkHttpRequest, next: H): O {
        attempt++
        request.subject.header(AMZ_SDK_INVOCATION_ID_HEADER, request.context.sdkInvocationId)
        onAttempt(request, attempt)
        return next.call(request)
    }

    private fun onAttempt(request: SdkHttpRequest, attempt: Int) {
        // setting ttl would never be accurate, just set what we know which is attempt and maybe max attempt
        val formattedMaxAttempts = maxAttempts?.let { "; max=$it" } ?: ""
        request.subject.header(AMZ_SDK_REQUEST_HEADER, "attempt=${attempt}$formattedMaxAttempts")
    }
}
