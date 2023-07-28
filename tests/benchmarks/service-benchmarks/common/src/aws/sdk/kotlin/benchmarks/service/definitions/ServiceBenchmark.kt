/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.DEFAULT_ITERATION_TIME
import aws.sdk.kotlin.benchmarks.service.DEFAULT_WARMUP_TIME
import aws.smithy.kotlin.runtime.client.SdkClient
import kotlin.time.Duration

interface ServiceBenchmark<C : SdkClient> {
    suspend fun client(): C
    suspend fun setup(client: C) { }
    val operations: List<OperationBenchmark<C>>
    suspend fun tearDown(client: C) { }
}

interface OperationBenchmark<C : SdkClient> {
    val name: String
    val warmupMode: RunMode get() = RunMode.Time(DEFAULT_WARMUP_TIME)
    val iterationMode: RunMode get() = RunMode.Time(DEFAULT_ITERATION_TIME)
    suspend fun setup(client: C) { }
    suspend fun transact(client: C)
    suspend fun tearDown(client: C) { }
}

sealed interface RunMode {
    data class Iterations(val iterations: Int) : RunMode
    data class Time(val time: Duration) : RunMode
}

abstract class AbstractOperationBenchmark<C : SdkClient>(override val name: String) : OperationBenchmark<C>
