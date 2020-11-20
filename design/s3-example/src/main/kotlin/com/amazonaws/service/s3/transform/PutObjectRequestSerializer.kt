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
package com.amazonaws.service.s3.transform

import com.amazonaws.service.s3.model.PutObjectRequest
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.HttpMethod
import software.aws.clientrt.http.feature.HttpSerialize
import software.aws.clientrt.http.feature.SerializationContext
import software.aws.clientrt.http.feature.SerializationProvider
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.request.headers
import software.aws.clientrt.http.request.url
import software.aws.clientrt.http.toHttpBody


class PutObjectRequestSerializer(val input: PutObjectRequest): HttpSerialize {
    override suspend fun serialize(builder: HttpRequestBuilder, serializationContext: SerializationContext) {
        // URI
        builder.method = HttpMethod.PUT
        builder.url {
            // NOTE: Individual serializers do not need to concern themselves with protocol/host
            path = "/putObject/${input.bucket}/${input.key}"
        }

        // Headers
        builder.headers {

            // optional header params
            if (input.cacheControl != null) append("Cache-Control", input.cacheControl)
            if (input.contentDisposition != null) append("Content-Disposition", input.contentDisposition)
            if (input.contentType != null) append("Content-Type", input.contentType)
            if (input.contentLength != null) append("Content-Length", input.contentLength.toString())
        }

        // FIXME - should Content-Type should be defaulted for bytestream payloads if not set (e.g. appliciation/octet-stream)?
        // FIXME - how do we want to deal with base64 encoded headers

        // payload
        builder.body = input.body?.toHttpBody() ?: HttpBody.Empty
    }
}

