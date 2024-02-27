/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.client.ProtocolResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.isSuccess
import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.response.copy
import aws.smithy.kotlin.runtime.serde.xml.*

/**
 * Interceptor to handle S3 responses with HTTP 200 status but have an error in the payload.
 *
 * See [S3 200 error](https://aws.amazon.com/premiumsupport/knowledge-center/s3-resolve-200-internalerror/)
 * and [aws-sdk-kotlin#199](https://github.com/awslabs/aws-sdk-kotlin/issues/199)
 */
internal object Handle200ErrorsInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeDeserialization(context: ProtocolResponseInterceptorContext<Any, HttpRequest, HttpResponse>): HttpResponse {
        val response = context.protocolResponse
        if (!response.status.isSuccess()) return response
        val payload = response.body.readAll() ?: return response
        val reader = xmlStreamReader(payload)
        val token = runCatching { reader.seek<XmlToken.BeginElement>() }.getOrNull()

        // according to the knowledge center article above we should treat these as 5xx,
        // our retry policy will handle standard error codes like `SlowDown`
        val statusCode = HttpStatusCode.InternalServerError
            .takeIf { token?.name?.local == "Error" }
            ?: response.status
        return response.copy(status = statusCode, body = HttpBody.fromBytes(payload))
    }
}
