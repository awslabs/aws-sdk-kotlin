/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk.http

import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.response.HttpResponse

/**
 * Default header name identifying the unique requestId
 */
const val X_AMZN_REQUEST_ID_HEADER = "X-Amzn-RequestId"

/**
 * Return a copy of the response with a new payload set
 */
fun HttpResponse.withPayload(payload: ByteArray?): HttpResponse {
    val newBody = if (payload != null) {
        ByteArrayContent(payload)
    } else {
        HttpBody.Empty
    }

    return HttpResponse(status, headers, newBody, request)
}
