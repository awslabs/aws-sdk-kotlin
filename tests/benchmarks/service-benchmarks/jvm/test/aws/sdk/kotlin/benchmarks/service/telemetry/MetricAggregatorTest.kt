/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.telemetry

import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val TOLERANCE = 0.005

class MetricAggregatorTest {
    @Test
    fun testSummarization() {
        val fibbonacis = listOf(1, 2, 3, 0, 1, 8, 5).map(Int::toDouble)
        val primes = listOf(23, 37, 5, 41, 31, 3, 17, 59, 11, 2, 19, 29, 7, 47, 43, 13, 53).map(Int::toDouble)
        val aggregator = MetricAggregator()

        // Interleave metric values
        (0..max(fibbonacis.size, primes.size)).forEach { idx ->
            if (idx < fibbonacis.size) aggregator.add("fibbonaci", fibbonacis[idx])
            if (idx < primes.size) aggregator.add("prime", primes[idx])
        }

        val summary = aggregator.summarizeAndClear()

        assertEquals(2, summary.size)

        val fibbonaciSummary = assertNotNull(summary["fibbonaci"])
        assertEquals(7, fibbonaciSummary.count)
        assertStats(0.0, 2.86, 2.0, 6.2, 7.82, 8.0, fibbonaciSummary.statistics)

        val primeSummary = assertNotNull(summary["prime"])
        assertEquals(17, primeSummary.count)
        assertStats(2.0, 25.88, 23.0, 49.4, 58.04, 59.0, primeSummary.statistics)
    }

    private fun assertStats(
        min: Double,
        avg: Double,
        med: Double,
        p90: Double,
        p99: Double,
        max: Double,
        stats: Map<String, Double>,
    ) {
        assertEquals(6, stats.size)
        assertEquals(min, stats["min"]!!, TOLERANCE)
        assertEquals(avg, stats["avg"]!!, TOLERANCE)
        assertEquals(med, stats["med"]!!, TOLERANCE)
        assertEquals(p90, stats["p90"]!!, TOLERANCE)
        assertEquals(p99, stats["p99"]!!, TOLERANCE)
        assertEquals(max, stats["max"]!!, TOLERANCE)
    }
}
