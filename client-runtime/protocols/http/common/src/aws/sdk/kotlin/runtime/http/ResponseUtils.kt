/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.InternalSdkApi
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.response.HttpResponse

/**
 * Default header name identifying the unique requestId
 */
public const val X_AMZN_REQUEST_ID_HEADER: String = "X-Amzn-RequestId"

/**
 * Return a copy of the response with a new payload set
 */
@InternalSdkApi
public fun HttpResponse.withPayload(payload: ByteArray?): HttpResponse {
    val newBody = if (payload != null) {
        ByteArrayContent(payload)
    } else {
        HttpBody.Empty
    }

    return HttpResponse(status, headers, newBody)
}
