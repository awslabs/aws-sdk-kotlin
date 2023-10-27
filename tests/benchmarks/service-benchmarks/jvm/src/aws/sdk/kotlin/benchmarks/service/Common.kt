/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service

import aws.sdk.kotlin.benchmarks.service.telemetry.BenchmarkTelemetryProvider
import aws.sdk.kotlin.benchmarks.service.telemetry.MetricAggregator
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.retries.StandardRetryStrategy
import aws.smithy.kotlin.runtime.util.Uuid

object Common {
    val metricAggregator = MetricAggregator()

    val noRetries = StandardRetryStrategy {
        maxAttempts = 1
    }

    @OptIn(ExperimentalApi::class)
    val telemetryProvider = BenchmarkTelemetryProvider(metricAggregator)

    fun random(prefix: String = "") = "$prefix${Uuid.random()}"
}
