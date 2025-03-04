/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.createQueue
import aws.sdk.kotlin.services.sqs.model.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.util.*
import kotlin.time.Duration.Companion.seconds

object SqsTestUtils {
    const val DEFAULT_REGION = "us-west-2"

    const val TEST_QUEUE_WRONG_CHECKSUM_PREFIX = "sqs-test-queue-"
    const val TEST_QUEUE_CORRECT_CHECKSUM_PREFIX = "sqs-test-queue-"

    const val TEST_MESSAGE_BODY = "Hello World"
    const val TEST_MESSAGE_ATTRIBUTES_NAME = "TestAttribute"
    const val TEST_MESSAGE_ATTRIBUTES_VALUE = "TestAttributeValue"
    const val TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE = "TestSystemAttributeValue"

    suspend fun getTestQueueUrl(
        client: SqsClient,
        prefix: String,
        region: String? = null,
    ): String = getQueueUrlWithPrefix(client, prefix, region)

    suspend fun getQueueUrlWithPrefix(
        client: SqsClient,
        prefix: String,
        region: String? = null,
    ): String = withTimeout(60.seconds) {
        val queueUrls = client.listQueues().queueUrls

        var matchingQueueUrl = queueUrls?.firstOrNull { url ->
            val queueUrl = URI(url).toURL()
            val hostParts = queueUrl.host.split(".")

            val regionMatches = if (region != null) {
                hostParts.getOrNull(1)?.equals(region, ignoreCase = true) ?: false
            } else {
                true
            }

            val queueName = queueUrl.path.split("/").last()
            val prefixMatches = queueName.startsWith(prefix)

            regionMatches && prefixMatches
        }

        if (matchingQueueUrl == null) {
            matchingQueueUrl = prefix + UUID.randomUUID()
            println("Creating Sqs queue: $matchingQueueUrl")

            client.createQueue {
                queueName = matchingQueueUrl
            }
        } else {
            println("Using existing Sqs queue: $matchingQueueUrl")
        }

        matchingQueueUrl
    }

    suspend fun deleteQueueAndAllMessages(client: SqsClient, queueUrl: String): Unit = coroutineScope {
        try {
            println("Purging Sqs queue: $queueUrl")
            val purgeRequest = PurgeQueueRequest {
                this.queueUrl = queueUrl
            }

            client.purgeQueue(purgeRequest)
            println("Queue purged successfully.")

            println("Deleting Sqs queue: $queueUrl")
            val deleteRequest = DeleteQueueRequest {
                this.queueUrl = queueUrl
            }

            client.deleteQueue(deleteRequest)
            println("Queue deleted successfully.")
        } catch (e: SqsException) {
            println("Error during delete SQS queue: ${e.message}")
        }
    }

    fun buildSendMessageBatchRequestEntry(batchId: Int): SendMessageBatchRequestEntry{
        return SendMessageBatchRequestEntry {
            id = batchId.toString()
            messageBody = TEST_MESSAGE_BODY + batchId
            messageAttributes = hashMapOf(
                TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                    dataType = "String"
                    stringValue = TEST_MESSAGE_ATTRIBUTES_VALUE + batchId
                }
            )
            messageSystemAttributes = hashMapOf(
                MessageSystemAttributeNameForSends.AwsTraceHeader to MessageSystemAttributeValue {
                    dataType = "String"
                    stringValue = TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE + batchId
                }
            )
        }
    }
}
