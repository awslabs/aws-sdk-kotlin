/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.model.*
import aws.sdk.kotlin.services.sqs.paginators.listQueuesPaginated
import aws.sdk.kotlin.services.sqs.paginators.queueUrls
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout
import java.util.*
import kotlin.time.Duration.Companion.seconds

object SqsTestUtils {
    const val DEFAULT_REGION = "us-west-2"

    const val TEST_QUEUE_PREFIX = "sqs-test-queue-"

    const val TEST_MESSAGE_BODY = "Hello World"
    const val TEST_MESSAGE_ATTRIBUTES_NAME = "TestAttribute"
    const val TEST_MESSAGE_ATTRIBUTES_VALUE = "TestAttributeValue"
    const val TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE = "TestSystemAttributeValue"

    suspend fun getTestQueueUrl(client: SqsClient, prefix: String): String =
        getQueueUrlWithPrefix(client, prefix)

    private suspend fun getQueueUrlWithPrefix(client: SqsClient, prefix: String): String = withTimeout(60.seconds) {
        var matchingQueueUrl = client
            .listQueuesPaginated { queueNamePrefix = prefix }
            .queueUrls()
            .firstOrNull()

        if (matchingQueueUrl == null) {
            matchingQueueUrl = prefix + UUID.randomUUID()
            println("Creating SQS queue: $matchingQueueUrl")

            client.createQueue {
                queueName = matchingQueueUrl
            }
        } else {
            println("Using existing SQS queue: $matchingQueueUrl")
        }

        matchingQueueUrl
    }

    suspend fun deleteQueueAndAllMessages(client: SqsClient, queueUrl: String) {
        try {
            println("Purging SQS queue: $queueUrl")

            client.purgeQueue(
                PurgeQueueRequest {
                    this.queueUrl = queueUrl
                },
            )

            println("Queue purged successfully.")

            println("Deleting SQS queue: $queueUrl")

            client.deleteQueue(
                DeleteQueueRequest {
                    this.queueUrl = queueUrl
                },
            )

            println("Queue deleted successfully.")
        } catch (e: SqsException) {
            println("Error during delete SQS queue: ${e.message}")
        }
    }

    fun buildSendMessageBatchRequestEntry(batchId: Int): SendMessageBatchRequestEntry = SendMessageBatchRequestEntry {
        id = batchId.toString()
        messageBody = TEST_MESSAGE_BODY + batchId
        messageAttributes = mapOf(
            TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                dataType = "String"
                stringValue = TEST_MESSAGE_ATTRIBUTES_VALUE + batchId
            },
        )
        messageSystemAttributes = mapOf(
            MessageSystemAttributeNameForSends.AwsTraceHeader to MessageSystemAttributeValue {
                dataType = "String"
                stringValue = TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE + batchId
            },
        )
    }
}
