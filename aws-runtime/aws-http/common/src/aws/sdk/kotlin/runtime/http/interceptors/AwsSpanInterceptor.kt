/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.tracing.TraceSpan
import aws.smithy.kotlin.runtime.tracing.traceSpan
import kotlin.coroutines.coroutineContext

/**
 * HTTP interceptor that sets AWS specific span attributes
 */
@InternalSdkApi
public object AwsSpanInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeAttemptCompletion(context: ResponseInterceptorContext<Any, Any, HttpRequest, HttpResponse?>): Result<Any> {
        // ensure each attempt span gets request ID attributes
        val httpResp = context.protocolResponse
        if (httpResp != null) {
            val span = coroutineContext.traceSpan
            setAwsRequestIdAttrs(span, httpResp)
        }
        return super.modifyBeforeAttemptCompletion(context)
    }

    override suspend fun modifyBeforeCompletion(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>): Result<Any> {
        // ensure the overall operation span gets request ID attributes
        val httpResp = context.protocolResponse
        if (httpResp != null) {
            val span = coroutineContext.traceSpan
            setAwsRequestIdAttrs(span, httpResp)
        }
        return super.modifyBeforeCompletion(context)
    }

    private fun setAwsRequestIdAttrs(span: TraceSpan, httpResp: HttpResponse) {
        // https://repost.aws/knowledge-center/s3-request-id-values
        // https://opentelemetry.io/docs/reference/specification/trace/semantic_conventions/instrumentation/aws-sdk/
        httpResp.headers["x-amz-request-id"]?.let {
            span.setAttr("aws.request_id", it)
        }
        httpResp.headers["x-amz-id-2"]?.let {
            span.setAttr("aws.request_id2", it)
        }
    }
}
