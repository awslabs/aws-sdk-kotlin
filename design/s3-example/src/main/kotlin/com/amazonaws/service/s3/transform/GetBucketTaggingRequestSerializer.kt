/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.s3.transform

import com.amazonaws.service.s3.model.GetBucketTaggingRequest
import software.aws.clientrt.http.HttpMethod
import software.aws.clientrt.http.feature.HttpSerialize
import software.aws.clientrt.http.feature.SerializationContext
import software.aws.clientrt.http.feature.SerializationProvider
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.request.url

class GetBucketTaggingRequestSerializer(val input: GetBucketTaggingRequest): HttpSerialize {
    override suspend fun serialize(builder: HttpRequestBuilder, serializationContext: SerializationContext) {
        // URI
        builder.method = HttpMethod.GET
        builder.url {
            // NOTE: Individual serializers do not need to concern themselves with protocol/host
            path = "/getObject/${input.bucket}?tagging"

            // Query parameters
        }

        // Headers

        // payload
    }
}
