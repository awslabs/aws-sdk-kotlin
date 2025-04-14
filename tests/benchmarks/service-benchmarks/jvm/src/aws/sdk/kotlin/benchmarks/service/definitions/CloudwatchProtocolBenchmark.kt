/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatch.model.*
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class CloudwatchProtocolBenchmark : ServiceProtocolBenchmark<CloudWatchClient> {
    companion object {
        val suiteId = UUID.randomUUID()
        val baseTime = System.currentTimeMillis() - 2 * 60 * 60 * 1000
        private inline fun Int.padded(): String = String.format("%03d", this)
    }

    @OptIn(ExperimentalApi::class)
    override suspend fun client() = CloudWatchClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
            connectTimeout = 30.seconds
        }
    }

    override val scales = listOf(16, 64, 256, 1000)

    override val operations get() = listOf(
        putMetricDataBenchmark,
        getMetricDataBenchmark,
        listMetricsBenchmark,
    )

    private val putMetricDataBenchmark = object : AbstractOperationProtocolBenchmark<CloudWatchClient>("Put metric data") {
        override val requireScaling = true

        override suspend fun transact(client: CloudWatchClient, scale: Int, iteration: Int) {
            val putMetricDataRequest = PutMetricDataRequest {
                namespace = "TestNameSpace"
                metricData = (0 until scale).map { metricDatumIndex ->
                    MetricDatum {
                        metricName = "TestMetric"
                        dimensions = listOf(
                            Dimension {
                                name = "TestDimension"
                                value = "$suiteId-$scale"
                            },
                        )
                        value = Random.nextDouble()
                        unit = null
                        timestamp = Instant.fromEpochMilliseconds(2000 * (metricDatumIndex + 1) + baseTime)
                    }
                }.toMutableList()
            }

            client.putMetricData(putMetricDataRequest)

            if ((iteration % 50) == 0) Thread.sleep(2000)
        }
    }

    private val getMetricDataBenchmark = object : AbstractOperationProtocolBenchmark<CloudWatchClient>("Get metric data") {
        override val requireScaling = true

        override suspend fun transact(client: CloudWatchClient, scale: Int, iteration: Int) {
            val getMetricDataRequest = GetMetricDataRequest {
                startTime = Instant.fromEpochMilliseconds(baseTime)
                endTime = Instant.fromEpochMilliseconds(baseTime + 3600000)
                metricDataQueries = listOf(
                    MetricDataQuery {
                        id = "m0"
                        returnData = true
                        metricStat {
                            unit = null
                            stat = "Sum"
                            metric = Metric {
                                namespace = "TestNamespace"
                                metricName = "TestMetric"
                                dimensions = listOf(
                                    Dimension {
                                        name = "TestDimension"
                                        value = "$suiteId-$scale"
                                    },
                                )
                            }
                            period = 60
                        }
                    },
                )
            }

            client.getMetricData(getMetricDataRequest)

            if ((iteration % 50) == 0) Thread.sleep(2000)
        }
    }

    private val listMetricsBenchmark = object : AbstractOperationProtocolBenchmark<CloudWatchClient>("List metrics") {
        override val requireScaling = true

        override suspend fun transact(client: CloudWatchClient, scale: Int, iteration: Int) {
            client.listMetrics(
                ListMetricsRequest {
                    namespace = "TestNamespace"
                },
            )

            if ((iteration % 50) == 0) Thread.sleep(2000)
        }
    }
}
