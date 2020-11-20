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
package com.amazonaws.service.lambda.transform

import com.amazonaws.service.lambda.model.InvokeRequest
import software.aws.clientrt.http.*
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.feature.HttpSerialize
import software.aws.clientrt.http.feature.SerializationContext
import software.aws.clientrt.http.feature.SerializationProvider
import software.aws.clientrt.http.request.*

class InvokeRequestSerializer(val input: InvokeRequest): HttpSerialize {
    override suspend fun serialize(builder: HttpRequestBuilder, serializationContext: SerializationContext) {
        // URI
        builder.method = HttpMethod.POST
        builder.url {
            // NOTE: Individual serializers do not need to concern themselves with protocol/host
            path = "/2015-03-31/functions/${input.functionName}/invocations"

            // Query Parameters
            if (input.qualifier != null) parameters.append("Qualifier", input.qualifier)
        }

        // Headers
        builder.headers {
            append("Content-Type", "application/x-amz-json-1.1")

            // optional header params
            if (input.invocationType != null) append("X-Amz-Invocation-Type", input.invocationType)
            if (input.logType != null) append("X-Amz-Log-Type", input.logType)
            if (input.clientContext != null) append("X-Amz-Client-Context", input.clientContext)
            append("X-Amz-Client-Token", input.idempotencyToken ?: serializationContext.idempotencyTokenProvider.generateToken())
        }

        // payload
        if (input.payload != null) {
            builder.body = ByteArrayContent(input.payload)
        }
    }
}
