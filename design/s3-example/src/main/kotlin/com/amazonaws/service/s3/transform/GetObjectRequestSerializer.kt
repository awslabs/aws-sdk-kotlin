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

import com.amazonaws.service.s3.model.GetObjectRequest
import software.aws.clientrt.http.HttpMethod
import software.aws.clientrt.http.feature.HttpSerialize
import software.aws.clientrt.http.feature.SerializationContext
import software.aws.clientrt.http.feature.SerializationProvider
import software.aws.clientrt.http.parameters
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.request.headers
import software.aws.clientrt.http.request.url


class GetObjectRequestSerializer(val input: GetObjectRequest): HttpSerialize {
    override suspend fun serialize(builder: HttpRequestBuilder, serializationContext: SerializationContext) {
        // URI
        builder.method = HttpMethod.GET
        builder.url {
            // NOTE: Individual serializers do not need to concern themselves with protocol/host
            path = "/getObject/${input.bucket}/${input.key}"

            // Query parameters
            parameters {
                if (input.partNumber != null) append("PartNumber", input.partNumber.toString())
                if (input.responseCacheControl != null) append("ResponseCacheControl", input.responseCacheControl)
                if (input.responseContentDisposition != null) append("ResponseContentDisposition", input.responseContentDisposition)
                if (input.responseContentEncoding != null) append("ResponseContentEncoding", input.responseContentEncoding)
                if (input.responseContentLanguage != null) append("ResponseContentLanguage", input.responseContentLanguage)
                if (input.responseContentType != null) append("ResponseContentType", input.responseContentType)
                if (input.responseExpires != null) append("ResponseExpires", input.responseExpires)
                if (input.versionId != null) append("VersionId", input.versionId)
            }
        }

        // Headers
        builder.headers {
            // optional header params
            if (input.ifMatch != null) append("If-Match", input.ifMatch)
            if (input.ifModifiedSince != null) append("If-Modified-Since", input.ifModifiedSince)
            if (input.ifNoneMatch != null) append("If-None-Match", input.ifNoneMatch)
            if (input.ifUnmodifiedSince != null) append("If-Unmodified-Since ", input.ifUnmodifiedSince)
            if (input.range != null) append("Range ", input.range)
            if (input.sseCustomerAlgorithm != null) append("x-amz-server-side-encryption-customer-algorithm", input.sseCustomerAlgorithm)
            if (input.sseCustomerKey != null) append("x-amz-server-side-encryption-customer-key", input.sseCustomerKey)
            if (input.sseCustomerKeyMd5 != null) append("x-amz-server-side-encryption-customer-key-MD5", input.sseCustomerKeyMd5)
            if (input.requestPayer != null) append("x-amz-request-payer", input.requestPayer)
        }

        // payload
    }
}

