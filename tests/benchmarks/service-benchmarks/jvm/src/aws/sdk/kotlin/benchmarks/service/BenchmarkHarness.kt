/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service

import aws.sdk.kotlin.benchmarks.service.definitions.*
import aws.sdk.kotlin.benchmarks.service.telemetry.MetricSummary
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.io.use
import kotlin.time.Duration.Companion.seconds

val DEFAULT_WARMUP_TIME = 5.seconds
val DEFAULT_ITERATION_TIME = 15.seconds

private val benchmarks = setOf(
    S3Benchmark(),
    SnsBenchmark(),
    StsBenchmark(),
    CloudwatchBenchmark(),
    CloudwatchEventsBenchmark(),
    DynamoDbBenchmark(),
    S3ExpressBenchmark(),
    SecretsManagerBenchmark(),
).map {
    @Suppress("UNCHECKED_CAST")
    it as ServiceBenchmark<SdkClient>
}

suspend fun main(args: Array<String>) {
    // FIXME: Unify service and protocol benchmark interfaces into a common base module
    // to support generic parameterized benchmarks and reduce duplication
    val useProtocolBenchmark = "protocol" in args

    if (useProtocolBenchmark) {
        val harness = ProtocolBenchmarkHarness()
        harness.execute()
    } else {
        val harness = BenchmarkHarness()
        harness.execute()
    }
}

class BenchmarkHarness {
    private val summaries = mutableMapOf<String, MutableMap<String, Map<String, MetricSummary>>>()

    suspend fun execute() {
        benchmarks.forEach { execute(it) }
        println()
        printResults()
    }

    private suspend fun execute(benchmark: ServiceBenchmark<SdkClient>) {
        benchmark.client().use { client ->
            println("${client.config.clientName}:")

            println("  Setting up...")
            benchmark.setup(client)

            try {
                benchmark.operations.forEach { execute(it, client) }
            } finally {
                benchmark.tearDown(client)
            }
        }
        println()
    }

    private suspend fun execute(operation: OperationBenchmark<SdkClient>, client: SdkClient) {
        println("  ${operation.name}:")

        println("    Setting up...")
        operation.setup(client)

        try {
            println("    Warming up for ${operation.warmupMode.explanation}...")
            operation.warmupMode.forAtLeast {
                operation.transact(client)
            }

            Common.metricAggregator.clear()

            println("    Measuring for ${operation.iterationMode.explanation}...")
            operation.iterationMode.forAtLeast {
                operation.transact(client)
            }

            val summary = Common.metricAggregator.summarizeAndClear()
            summaries.getOrPut(client.config.clientName, ::mutableMapOf)[operation.name] = summary
        } finally {
            println("    Tearing down...")
            operation.tearDown(client)
        }
    }

    private fun printResults() {
        val table = ResultsTable.from(summaries)
        println(table)
    }
}
