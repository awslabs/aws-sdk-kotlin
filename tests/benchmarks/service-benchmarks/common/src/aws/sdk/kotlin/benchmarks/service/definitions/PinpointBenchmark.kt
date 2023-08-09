package aws.sdk.kotlin.benchmarks.service.definitions

import aws.sdk.kotlin.benchmarks.service.Common
import aws.sdk.kotlin.services.pinpoint.*
import aws.sdk.kotlin.services.pinpoint.model.ChannelType
import aws.sdk.kotlin.services.pinpoint.model.Event
import aws.sdk.kotlin.services.pinpoint.model.EventsBatch
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.time.Instant

class PinpointBenchmark : ServiceBenchmark<PinpointClient> {
    private val epAddress = Common.random("sdk-benchmark-address-")
    private lateinit var appId: String
    private val epId = Common.random("sdk-benchmark-endpoint-")

    @OptIn(ExperimentalApi::class)
    override suspend fun client() = PinpointClient.fromEnvironment {
        retryStrategy = Common.noRetries
        telemetryProvider = Common.telemetryProvider
        httpClient {
            telemetryProvider = Common.telemetryProvider
        }
    }

    override suspend fun setup(client: PinpointClient) {
        val resp = client.createApp {
            createApplicationRequest {
                name = Common.random("sdk-benchmark-app-")
            }
        }

        appId = resp.applicationResponse!!.id!!

        client.updateEndpoint {
            applicationId = appId
            endpointId = epId
            endpointRequest {
                address = epAddress
                channelType = ChannelType.InApp
            }
        }
    }

    override val operations get() = listOf(getEndpointBenchmark, putEventsBenchmark)

    override suspend fun tearDown(client: PinpointClient) {
        client.deleteEndpoint {
            applicationId = appId
            endpointId = epId
        }

        client.deleteApp {
            applicationId = appId
        }
    }

    private val getEndpointBenchmark = object : AbstractOperationBenchmark<PinpointClient>("GetEndpoint") {
        override suspend fun transact(client: PinpointClient) {
            client.getEndpoint {
                applicationId = appId
                endpointId = epId
            }
        }
    }

    private val putEventsBenchmark = object : AbstractOperationBenchmark<PinpointClient>("PutEvents") {
        override suspend fun transact(client: PinpointClient) {
            client.putEvents {
                applicationId = appId
                eventsRequest {
                    batchItem = mapOf(
                        Common.random() to EventsBatch {
                            endpoint {
                                address = epAddress
                            }
                            events = mapOf(
                                "foo" to Event {
                                    eventType = "Bar"
                                    timestamp = Instant.now().toString()
                                    attributes = mapOf("baz" to "qux")
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
