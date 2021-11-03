/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.services.machinelearning.internal

import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.services.machinelearning.model.MachineLearningException
import aws.sdk.kotlin.services.machinelearning.model.PredictRequest
import aws.smithy.kotlin.runtime.http.Feature
import aws.smithy.kotlin.runtime.http.FeatureKey
import aws.smithy.kotlin.runtime.http.HttpClientFeatureFactory
import aws.smithy.kotlin.runtime.http.middleware.setRequestEndpoint
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation

internal class ResolvePredictEndpoint : Feature {
    companion object Feature : HttpClientFeatureFactory<Unit, ResolvePredictEndpoint> {
        override val key: FeatureKey<ResolvePredictEndpoint> = FeatureKey("ResolvePredictEndpoint")
        override fun create(block: Unit.() -> Unit): ResolvePredictEndpoint = ResolvePredictEndpoint()
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.initialize.intercept { req, next ->
            val input = req.subject as PredictRequest
            if (input.predictEndpoint == null || input.predictEndpoint.isBlank()) {
                throw MachineLearningException("Predict requires predictEnpoint to be set to a non-empty value")
            }
            // Stash the endpoint for later use by the mutate interceptor
            req.context.predictEndpoint = AwsEndpoint(input.predictEndpoint)

            next.call(req)
        }

        operation.execution.mutate.intercept { req, next ->
            // This should've been set by the initialize interceptor
            val endpoint = req.context.predictEndpoint
            requireNotNull(endpoint) { "Predict endpoint wasn't set by middleware." }
            setRequestEndpoint(req, endpoint.endpoint)

            next.call(req)
        }
    }
}
