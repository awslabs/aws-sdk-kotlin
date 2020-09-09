/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
