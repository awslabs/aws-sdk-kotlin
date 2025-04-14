/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.DEFAULT_ITERATIONS
import aws.sdk.kotlin.benchmarks.service.DEFAULT_WARMUP_ITERATIONS
import aws.smithy.kotlin.runtime.client.SdkClient

/**
 * Extends ServiceBenchmark with scaling capabilities.
 * @param C The type of the specific service client
 */
interface ServiceProtocolBenchmark<C : SdkClient> : ServiceBenchmark<C> {
    /**
     * List of scale factors to use in benchmarking.
     */
    val scales: List<Int>

    /**
     * Override operations to use ScalableOperationBenchmark
     */
    override val operations: List<OperationProtocolBenchmark<C>>
}

/**
 * Extends OperationBenchmark with scaling capabilities.
 */
interface OperationProtocolBenchmark<C : SdkClient> : OperationBenchmark<C> {
    /**
     * Indicates whether this operation requires scaling.
     */
    val requireScaling: Boolean

    /**
     * The [RunMode] to use while warming up. The default is `RunMode.Iterations(DEFAULT_WARMUP_ITERATIONS)`.
     */
    override val warmupMode: RunMode get() = RunMode.Iterations(DEFAULT_WARMUP_ITERATIONS)

    /**
     * The [RunMode] to use while iterating on the actual benchmark. The default is
     * `RunMode.Iterations(DEFAULT_ITERATIONS)`.
     */
    override val iterationMode: RunMode get() = RunMode.Iterations(DEFAULT_ITERATIONS)

    /**
     * Extended transact method with scale and iteration parameters.
     * @param client The service client to use
     * @param scale The scale factor for this transaction
     * @param iteration The current iteration number
     */
    suspend fun transact(client: C, scale: Int, iteration: Int)

    override suspend fun transact(client: C) { }
}

/**
 * An abstract base class for operation protocol benchmarks.
 * @param name The name of the operation (e.g., `HeadBucket`).
 * @property requireScaling Indicates whether this operation requires scaling for benchmarking.
 *  - When true, the operation will be executed with different scale factors to measure performance under varying loads.
 *  - When false, the operation will be executed with default scale (1).
 */
abstract class AbstractOperationProtocolBenchmark<C : SdkClient>(
    override val name: String,
    override val requireScaling: Boolean = false,
) : OperationProtocolBenchmark<C>
