/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.http.operation.ModifyRequestMiddleware
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.util.EnvironmentProvider
import aws.smithy.kotlin.runtime.util.PlatformProvider
import aws.smithy.kotlin.runtime.util.text.percentEncodeTo

internal const val ENV_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME"
internal const val ENV_TRACE_ID = "_X_AMZN_TRACE_ID"
internal const val HEADER_TRACE_ID = "X-Amzn-Trace-Id"

/**
 * HTTP middleware to add the recursion detection header where required.
 */
@InternalSdkApi
public class RecursionDetection(
    private val env: EnvironmentProvider = PlatformProvider.System,
) : ModifyRequestMiddleware {
    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        if (req.subject.headers.contains(HEADER_TRACE_ID)) return req

        val traceId = env.getenv(ENV_TRACE_ID)
        if (env.getenv(ENV_FUNCTION_NAME) == null || traceId == null) return req

        req.subject.headers[HEADER_TRACE_ID] = traceId.percentEncode()
        return req
    }
}

/**
 * Percent-encode ISO control characters for the purposes of this specific header.
 *
 * The existing `Char::isISOControl` check cannot be used here, because that matches against characters in
 * `[0x00, 0x1f] U [0x7f, 0x9f]`. The SEP for recursion detection dictates we should only encode across
 * `[0x00, 0x1f]`.
 */
private fun String.percentEncode(): String {
    val sb = StringBuilder(this.length)
    val data = this.encodeToByteArray()
    for (cbyte in data) {
        val chr = cbyte.toInt().toChar()
        if (chr.code in 0x00..0x1f) {
            cbyte.percentEncodeTo(sb)
        } else {
            sb.append(chr)
        }
    }
    return sb.toString()
}
