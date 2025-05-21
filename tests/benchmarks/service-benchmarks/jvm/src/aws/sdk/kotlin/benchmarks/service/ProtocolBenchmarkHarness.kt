/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service

import aws.sdk.kotlin.benchmarks.service.definitions.*
import aws.sdk.kotlin.benchmarks.service.telemetry.MetricSummary
import aws.smithy.kotlin.runtime.client.SdkClient
import aws.smithy.kotlin.runtime.io.use

const val DEFAULT_ITERATIONS = 500
const val DEFAULT_WARMUP_ITERATIONS = 100

private val protocolBenchmarks = setOf(
    CloudwatchProtocolBenchmark(),
    SecretsManagerProtocolBenchmark(),
).map {
    @Suppress("UNCHECKED_CAST")
    it as ServiceProtocolBenchmark<SdkClient>
}

class ProtocolBenchmarkHarness {
    private val summaries = mutableMapOf<String, MutableMap<String, Map<String, MetricSummary>>>()

    suspend fun execute() {
        protocolBenchmarks.forEach { execute(it) }
        println()
        printResults()
    }

    private suspend fun execute(benchmark: ServiceProtocolBenchmark<SdkClient>) {
        benchmark.client().use { client ->
            println("${client.config.clientName}:")

            println("  Setting up...")
            benchmark.setup(client)

            try {
                benchmark.operations.forEach { execute(it, client, benchmark.scales) }
            } finally {
                benchmark.tearDown(client)
            }
        }
        println()
    }

    private suspend fun execute(operation: OperationProtocolBenchmark<SdkClient>, client: SdkClient, scales: List<Int>) {
        println("  ${operation.name}:")

        println("    Setting up...")
        operation.setup(client)

        try {
            if (operation.requireScaling) {
                for (scale in scales) {
                    println("    Warming up for ${operation.warmupMode.explanation} (scale: $scale)...")
                    operation.warmupMode.forAtLeast { iteration ->
                        operation.transact(client, scale, iteration!!)
                    }
                }
            } else {
                println("    Warming up for ${operation.warmupMode.explanation}...")
                operation.warmupMode.forAtLeast { iteration ->
                    operation.transact(client, 0, iteration!!)
                }
            }

            Common.metricAggregator.clear()

            if (operation.requireScaling) {
                for (scale in scales) {
                    println("    Measuring for ${operation.iterationMode.explanation} (scale: $scale)...")
                    operation.iterationMode.forAtLeast { iteration ->
                        operation.transact(client, scale, iteration!!)
                    }
                }
            } else {
                println("    Measuring for ${operation.iterationMode.explanation}...")
                operation.iterationMode.forAtLeast { iteration ->
                    operation.transact(client, 0, iteration!!)
                }
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
