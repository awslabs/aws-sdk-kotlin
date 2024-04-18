/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.response.toBuilder
import aws.smithy.kotlin.runtime.time.Instant

/**
 * Interceptor to handle special-cased `Expires` field which must not cause deserialization to fail.
 */
internal object ExpiresFieldInterceptor: HttpInterceptor {
    override suspend fun modifyBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>): HttpResponse {
        val response = context.protocolResponse.toBuilder()
        val responseHeaders = context.protocolResponse.headers

        if (responseHeaders.contains("Expires")) {
            // if parsing `Expires` would fail, remove the header value so it deserializes to `null`
            try {
                Instant.fromRfc5322(responseHeaders["Expires"]!!)
            } catch (e: Exception) {
                response.headers.remove("Expires")
            }
        }

        return response.build()
    }
}
