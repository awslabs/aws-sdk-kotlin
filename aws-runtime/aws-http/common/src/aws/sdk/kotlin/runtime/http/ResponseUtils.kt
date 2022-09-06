/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.category
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.isSuccess
import aws.smithy.kotlin.runtime.http.response.HttpResponse

/**
 * Default header name identifying the unique requestId
 * See https://aws.amazon.com/premiumsupport/knowledge-center/s3-request-id-values
 */
public const val X_AMZN_REQUEST_ID_HEADER: String = "x-amz-request-id"

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

// Provides the policy of what constitutes a status code match in service response
@InternalSdkApi
public fun HttpStatusCode.matches(expected: HttpStatusCode?): Boolean =
    expected == this || (expected == null && this.isSuccess()) || expected?.category() == this.category()
