/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.kinesis

import aws.sdk.kotlin.services.kinesis.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for Kinesis SubscribeToShard (an RPC-bound protocol)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KinesisSubscribeToShardTest {
    private val client = KinesisClient { region = "us-east-1" }

    private val STREAM_NAME_PREFIX = "aws-sdk-kotlin-e2e-test-stream"
    private val STREAM_CONSUMER_NAME_PREFIX = "aws-sdk-kotlin-e2e-test"

    private val TEST_DATA = "Bees, bees, bees, bees!"

    private lateinit var dataStreamArn: String
    private lateinit var dataStreamConsumerArn: String

    /**
     * Create infrastructure required for the test, if it doesn't exist already.
     */
    @BeforeAll
    fun setup(): Unit = runBlocking {
        client.getOrCreateStream()
        client.getOrRegisterStreamConsumer()
    }

    /**
     * Delete infrastructure used for the test.
     */
    @AfterAll
    fun cleanUp(): Unit = runBlocking {
        client.deregisterStreamConsumer {
            streamArn = dataStreamArn
            consumerArn = dataStreamConsumerArn
        }

        client.deleteStream {
            streamArn = dataStreamArn
        }
    }

    /**
     * Select the single shard ID associated with the data stream, and subscribe to it.
     * Read one event and make sure the data matches what's expected.
     */
    @Test
    fun testSubscribeToShard() = runBlocking {
        val dataStreamShardId = client.listShards {
            streamArn = dataStreamArn
        }.shards?.single()!!.shardId

        client.subscribeToShard(
            SubscribeToShardRequest {
                consumerArn = dataStreamConsumerArn
                shardId = dataStreamShardId
                startingPosition = StartingPosition {
                    type = ShardIteratorType.TrimHorizon
                }
            },
        ) {
            val event = it.eventStream?.first()
            val record = event?.asSubscribeToShardEvent()?.records?.single()
            println("Got ${record?.data?.decodeToString()}")
            assertEquals(TEST_DATA, record?.data?.decodeToString())
        }
    }

    /**
     * Get a Kinesis data stream with the [STREAM_NAME_PREFIX], or if one does not exist,
     * create one and populate it with one test record.
     *
     * Assigns a value to the `lateinit var` [dataStreamArn]
     */
    private suspend fun KinesisClient.getOrCreateStream() {
        var listStreamsResp = listStreams { }

        dataStreamArn = listStreamsResp.streamSummaries?.find { it.streamName?.startsWith(STREAM_NAME_PREFIX) ?: false }?.streamArn ?: run {
            val randomStreamName = STREAM_NAME_PREFIX + UUID.randomUUID()
            createStream {
                streamName = randomStreamName
                shardCount = 1
            }
            delay(10.seconds) // creating a stream is asynchronous...

            listStreamsResp = listStreams { }

            val newStreamArn = listStreamsResp
                .streamSummaries
                ?.find { it.streamName?.startsWith(STREAM_NAME_PREFIX) ?: false }
                ?.streamArn ?: fail("Failed to create stream")

            putRecord {
                data = TEST_DATA.encodeToByteArray()
                streamArn = newStreamArn
                partitionKey = "Goodbye"
            }

            newStreamArn
        }
    }

    /**
     * Get a Kinesis data stream consumer, or if it doesn't exist, register a new one.
     *
     * Assigns a value to the `lateinit var` [dataStreamConsumerArn]
     */
    private suspend fun KinesisClient.getOrRegisterStreamConsumer() {
        val listStreamConsumersResp = listStreamConsumers {
            streamArn = dataStreamArn
        }

        dataStreamConsumerArn = listStreamConsumersResp.consumers?.firstOrNull { it.consumerName?.startsWith(STREAM_CONSUMER_NAME_PREFIX) ?: false }?.consumerArn ?: run {
            val randomConsumerName = STREAM_CONSUMER_NAME_PREFIX + UUID.randomUUID()
            val registerStreamConsumerResp = registerStreamConsumer {
                consumerName = randomConsumerName
                streamArn = dataStreamArn
            }
            delay(10.seconds) // registering a consumer is asynchronous... (status needs to be ACTIVE)

            registerStreamConsumerResp.consumer?.consumerArn ?: fail("Failed to register consumer")
        }
    }
}
