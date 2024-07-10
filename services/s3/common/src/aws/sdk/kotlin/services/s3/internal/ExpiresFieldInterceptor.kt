/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.response.toBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.coroutines.coroutineContext

/**
 * Interceptor to handle special-cased `Expires` field which must not cause deserialization to fail.
 */
internal object ExpiresFieldInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>): HttpResponse {
        val response = context.protocolResponse.toBuilder()

        if (response.headers.contains("Expires")) {
            response.headers["ExpiresString"] = response.headers["Expires"]!!

            // if parsing `Expires` would fail, remove the header value so it deserializes to `null`
            try {
                Instant.fromRfc5322(response.headers["Expires"]!!)
            } catch (e: Exception) {
                coroutineContext.logger<ExpiresFieldInterceptor>().warn {
                    "Failed to parse `expires`=\"${response.headers["Expires"]}\" as a timestamp, setting it to `null`. The unparsed value is available in `expiresString`."
                }
                response.headers.remove("Expires")
            }
        }

        return response.build()
    }
}
