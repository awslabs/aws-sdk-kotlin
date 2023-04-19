/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.machinelearning.internal

import aws.sdk.kotlin.services.machinelearning.model.MachineLearningException
import aws.sdk.kotlin.services.machinelearning.model.PredictRequest
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.operation.SdkHttpRequest
import aws.smithy.kotlin.runtime.http.operation.setResolvedEndpoint
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder

internal class ResolvePredictEndpoint : HttpInterceptor {
    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        @Suppress("UNCHECKED_CAST")
        val input = context.request as PredictRequest
        if (input.predictEndpoint.isNullOrBlank()) {
            throw MachineLearningException("Predict requires predictEndpoint to be set to a non-empty value")
        }
        val endpoint = Endpoint(input.predictEndpoint)
        val req = SdkHttpRequest(context.executionContext, context.protocolRequest.toBuilder())
        setResolvedEndpoint(req, endpoint)
        return req.subject.build()
    }
}
