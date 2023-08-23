/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.kinesis

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.fail
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import aws.sdk.kotlin.services.kinesis.*
import aws.sdk.kotlin.services.kinesis.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.first

/**
 * Tests for Kinesis SubscribeToShard (an RPC-bound protocol)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KinesisSubscribeToShardTest {

    private val client = KinesisClient { region = "us-east-1" }
    private val STREAM_NAME = "aws-sdk-kotlin-e2e-test-stream"
    private val TEST_DATA = "Bees, bees, bees, bees!"

    private lateinit var dataStreamArn: String
    private lateinit var dataStreamConsumerArn: String

    @BeforeAll
    fun setup(): Unit = runBlocking {
        client.createStream {
            streamName = STREAM_NAME
            shardCount = 1
        }

        // creating a stream is asynchronous...
        delay(10.seconds)

        val listStreamsResp = client.listStreams {}

        if (listStreamsResp.streamSummaries == null) {
            fail("Failed to create stream")
        }

        dataStreamArn = listStreamsResp.streamSummaries.single { it.streamName == STREAM_NAME }.streamArn!!

        val registerStreamConsumerResp = client.registerStreamConsumer {
            consumerName = "aws-sdk-kotlin-e2e-test"
            streamArn = dataStreamArn
        }

        // registering a consumer is asynchronous... (status needs to be ACTIVE)
        delay(10.seconds)

        dataStreamConsumerArn = registerStreamConsumerResp.consumer!!.consumerArn!!

        client.putRecord {
            data = TEST_DATA.encodeToByteArray()
            streamArn = dataStreamArn
            partitionKey = "Goodbye"
        }
    }

    @AfterAll
    fun cleanUp(): Unit = runBlocking {
        client.deregisterStreamConsumer {
            streamArn = dataStreamArn
            consumerArn = dataStreamConsumerArn
        }

        client.deleteStream {
            streamArn = dataStreamArn
            streamName = STREAM_NAME
        }
    }

    @Test
    fun testSubscribeToShard() = runBlocking {
        val dataStreamShardId = client.listShards {
            streamArn = dataStreamArn
        }.shards?.single()!!.shardId

        client.subscribeToShard(SubscribeToShardRequest {
            consumerArn = dataStreamConsumerArn
            shardId = dataStreamShardId
            startingPosition = StartingPosition {
                type = ShardIteratorType.TrimHorizon
            }
        }) {
            val event = it.eventStream?.first()
            val record = event?.asSubscribeToShardEvent()?.records?.single()
            println("Got ${record?.data?.decodeToString()}")
            assertEquals(TEST_DATA, record?.data?.decodeToString())
        }
    }
}
