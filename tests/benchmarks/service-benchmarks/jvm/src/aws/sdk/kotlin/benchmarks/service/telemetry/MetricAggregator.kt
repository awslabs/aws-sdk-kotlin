/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.telemetry

import kotlin.math.roundToInt

private const val P_SCALE = 1000

class MetricAggregator {
    private var builder = ConcurrentListBuilder<Metric>()

    fun add(name: String, value: Double) = builder.add(Metric(name, value))

    fun clear() {
        builder = ConcurrentListBuilder()
    }

    fun summarizeAndClear(): Map<String, MetricSummary> {
        val metrics = builder.toList()
        clear()
        return metrics
            .groupBy(Metric::name, Metric::value)
            .mapValues { (_, values) -> MetricSummary(values) }
    }

    private data class Metric(val name: String, val value: Double)
}

data class MetricSummary(val count: Int, val statistics: Map<String, Double>) {
    constructor(values: List<Double>) : this(values.size, values.summarize())
}

private fun List<Double>.summarize() = buildMap {
    val values = sorted()
    put("min", values.first())
    put("avg", values.average())
    put("med", values p 0.5)
    put("p90", values p 0.9)
    put("p99", values p 0.99)
    put("max", values.last())
}

infix fun List<Double>.p(percentile: Double): Double {
    val k = (P_SCALE * percentile * (size - 1)).roundToInt()
    val leftIdx = k / P_SCALE
    return when (val mod = k.mod(P_SCALE)) {
        0 -> this[leftIdx]
        else -> {
            val rightScale = mod.toDouble() / P_SCALE
            val leftScale = 1 - rightScale
            this[leftIdx] * leftScale + this[leftIdx + 1] * rightScale
        }
    }
}
