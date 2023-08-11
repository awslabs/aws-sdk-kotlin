/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.telemetry

import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.telemetry.TelemetryProvider
import aws.smithy.kotlin.runtime.telemetry.context.Context
import aws.smithy.kotlin.runtime.telemetry.context.ContextManager
import aws.smithy.kotlin.runtime.telemetry.logging.LoggerProvider
import aws.smithy.kotlin.runtime.telemetry.metrics.*
import aws.smithy.kotlin.runtime.telemetry.trace.TracerProvider
import aws.smithy.kotlin.runtime.util.Attributes

private val capturedMetrics = mapOf(
    "smithy.client.attempt_overhead_duration" to "Overhead",
    // "smithy.client.http.time_to_first_byte" to "TTFB",
    // "smithy.client.attempt_duration" to "Call",
    // "smithy.client.serialization_duration" to "Serlz",
    // "smithy.client.deserialization_duration" to "Deserlz",
    // "smithy.client.resolve_endpoint_duration" to "EPR",
)

@ExperimentalApi
class BenchmarkTelemetryProvider(private val metricAggregator: MetricAggregator) : TelemetryProvider {
    override val contextManager = ContextManager.None
    override val loggerProvider = LoggerProvider.None
    override val tracerProvider = TracerProvider.None

    override val meterProvider = object : MeterProvider {
        override fun getOrCreateMeter(scope: String) = object : Meter {
            override fun createUpDownCounter(name: String, units: String?, description: String?) =
                NoOpUpDownCounter

            override fun createAsyncUpDownCounter(
                name: String,
                callback: LongUpDownCounterCallback,
                units: String?,
                description: String?,
            ) = NoOpAsyncMeasurementHandle

            override fun createMonotonicCounter(name: String, units: String?, description: String?) =
                NoOpMonotonicCounter

            override fun createLongHistogram(name: String, units: String?, description: String?) =
                NoOpLongHistogram

            override fun createDoubleHistogram(name: String, units: String?, description: String?) =
                capturedMetrics[name]?.let { BenchmarkDoubleHistogram(it, units) } ?: NoOpDoubleHistogram

            override fun createLongGauge(
                name: String,
                callback: LongGaugeCallback,
                units: String?,
                description: String?,
            ) = NoOpAsyncMeasurementHandle

            override fun createDoubleGauge(
                name: String,
                callback: DoubleGaugeCallback,
                units: String?,
                description: String?,
            ) = NoOpAsyncMeasurementHandle
        }
    }

    private inner class BenchmarkDoubleHistogram(name: String, units: String?) : DoubleHistogram {
        private val newUnit: String?
        private val transform: (Double) -> Double

        init {
            when (units) {
                "s" -> {
                    newUnit = "ms"
                    transform = { it * 1000 }
                }

                null -> {
                    newUnit = null
                    transform = { it }
                }

                else -> throw IllegalArgumentException("Unknown unit type $units")
            }
        }

        private val formattedName = name + (newUnit?.let { " ($it)" } ?: "")

        override fun record(value: Double, attributes: Attributes, context: Context?) {
            metricAggregator.add(formattedName, transform(value))
        }
    }
}

private object NoOpAsyncMeasurementHandle : AsyncMeasurementHandle {
    override fun stop() { }
}

private object NoOpDoubleHistogram : DoubleHistogram {
    override fun record(value: Double, attributes: Attributes, context: Context?) { }
}

private object NoOpLongHistogram : LongHistogram {
    override fun record(value: Long, attributes: Attributes, context: Context?) { }
}

private object NoOpMonotonicCounter : MonotonicCounter {
    override fun add(value: Long, attributes: Attributes, context: Context?) { }
}

private object NoOpUpDownCounter : UpDownCounter {
    override fun add(value: Long, attributes: Attributes, context: Context?) { }
}
