/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.services.machinelearning.internal

import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.util.AttributeKey

internal val predictEndpointKey = AttributeKey<AwsEndpoint>("PredictEndpointKey")

internal var ExecutionContext.predictEndpoint: AwsEndpoint?
    get() = getOrNull(predictEndpointKey)
    set(value) = set(predictEndpointKey, value!!)
