/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.services.machinelearning.internal

import aws.sdk.kotlin.services.machinelearning.model.PredictRequest
import aws.sdk.kotlin.services.machinelearning.transform.PredictOperationSerializer
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder

internal class EndpointCustomizingPredictOperationSerializer : PredictOperationSerializer() {
    override suspend fun serialize(context: ExecutionContext, input: PredictRequest): HttpRequestBuilder {
        require(input.predictEndpoint != null) { "Missing required predictEndpoint argument" }
        val predictEndpointUrl = Url.parse(input.predictEndpoint)

        val builder = super.serialize(context, input)
        builder.url.host = predictEndpointUrl.host
        builder.url.port = predictEndpointUrl.port
        builder.url.path = predictEndpointUrl.path
        builder.headers["Host"] = predictEndpointUrl.host
        return builder
    }
}
