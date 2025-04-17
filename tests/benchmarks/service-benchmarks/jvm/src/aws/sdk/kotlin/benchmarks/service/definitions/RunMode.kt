/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Identifies the mode in which to run a phase of benchmark execution.
 */
sealed interface RunMode {
    /**
     * Run for a specific number of iterations.
     * @param iterations The number of iterations to run.
     */
    data class Iterations(val iterations: Int) : RunMode

    /**
     * Run for a specific amount of time.
     * @param time The amount of time to run.
     */
    data class Time(val time: Duration) : RunMode
}

val RunMode.explanation get() = when (this) {
    is RunMode.Iterations -> "$iterations iterations"
    is RunMode.Time -> time.toString()
}

internal inline fun RunMode.forAtLeast(block: (Int?) -> Unit) {
    val start = TimeSource.Monotonic.markNow()

    when (this) {
        is RunMode.Time -> {
            var cnt = 0
            while (start.elapsedNow() < time) {
                block(cnt)
                cnt++
            }
            println("      (completed $cnt iterations)")
        }

        is RunMode.Iterations -> {
            repeat(iterations) { cnt ->
                block(cnt + 1)
            }
            println("      (took ${start.elapsedNow()})")
        }
    }
}
