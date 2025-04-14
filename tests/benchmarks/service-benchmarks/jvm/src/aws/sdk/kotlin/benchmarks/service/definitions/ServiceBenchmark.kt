/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.DEFAULT_ITERATION_TIME
import aws.sdk.kotlin.benchmarks.service.DEFAULT_WARMUP_TIME
import aws.smithy.kotlin.runtime.client.SdkClient
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Defines the harness for conducting a benchmark of a service client.
 * @param C The type of the specific service client (e.g., [aws.sdk.kotlin.services.s3.S3Client]).
 */
interface ServiceBenchmark<C : SdkClient> {
    /**
     * Return a configured service client. This method **MUST NOT** perform any additional service setup.
     * @return A configured service client.
     */
    suspend fun client(): C

    /**
     * Sets up a service for benchmarking. This may involve creating/configuring specific resources (e.g., creating an
     * S3 bucket into which the benchmarks will read/write objects). Resources created/modified by this method
     * **SHOULD** be removed/restored by [tearDown].
     *
     * The default implementation of this method does nothing.
     * @param client The service client to use.
     */
    suspend fun setup(client: C) { }

    /**
     * The list of operations to benchmark.
     */
    val operations: List<OperationBenchmark<C>>

    /**
     * Cleans up a service after benchmarking. This may involve deleting specific resources (e.g., removing an S3 bucket
     * which was created for the purpose of benchmarking). This method will be called regardless of whether an exception
     * occurred during benchmarking.
     *
     * The default implementation of this method does nothing.
     * @param client The service client to use.
     */
    suspend fun tearDown(client: C) { }
}

/**
 * Defines the harness for conducting a benchmark of a specific service operation.
 */
interface OperationBenchmark<C : SdkClient> {
    /**
     * The name of the operation (e.g., `HeadBucket`).
     */
    val name: String

    /**
     * The [RunMode] to use while warming up. The default is `RunMode.Time(DEFAULT_WARMUP_TIME)`.
     */
    val warmupMode: RunMode get() = RunMode.Time(DEFAULT_WARMUP_TIME)

    /**
     * The [RunMode] to use while iterating on the actual benchmark. The default is
     * `RunMode.Time(DEFAULT_ITERATION_TIME)`.
     */
    val iterationMode: RunMode get() = RunMode.Time(DEFAULT_ITERATION_TIME)

    /**
     * Sets up an operation for benchmarking. This may involve creating/configuring specific resources (e.g., creating
     * an IAM role for use in an STS AssumeRole benchmark). Resources created/modified by this method **SHOULD** be
     * removed/restored by [tearDown].
     * @param client The service client to use.
     */
    suspend fun setup(client: C) { }

    /**
     * Perform a single service operation. This method **SHOULD** only perform a single service call and perform
     * minimal/no validation. This method will be called repeatedly during the warmup and iteration phase.
     * @param client The service client to use.
     */
    suspend fun transact(client: C)

    /**
     * Cleans up an operation after benchmarking. This may involve deleting specific resources (e.g., removing an IAM
     * role). This method will be called regardless of whether an exception occurred during benchmarking.
     *
     * The default implementation of this method does nothing.
     * @param client The service client to use.
     */
    suspend fun tearDown(client: C) { }
}

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

/**
 * An abstract base class for operation benchmarks.
 * @param The name of the operation (e.g., `HeadBucket`).
 */
abstract class AbstractOperationBenchmark<C : SdkClient>(override val name: String) : OperationBenchmark<C>
