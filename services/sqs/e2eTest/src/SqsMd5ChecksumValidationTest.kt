/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.e2etest

import aws.sdk.kotlin.e2etest.SqsTestUtils.DEFAULT_REGION
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_MESSAGE_ATTRIBUTES_NAME
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_MESSAGE_ATTRIBUTES_VALUE
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_MESSAGE_BODY
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_QUEUE_CORRECT_CHECKSUM_PREFIX
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_QUEUE_WRONG_CHECKSUM_PREFIX
import aws.sdk.kotlin.e2etest.SqsTestUtils.buildSendMessageBatchRequestEntry
import aws.sdk.kotlin.e2etest.SqsTestUtils.deleteQueueAndAllMessages
import aws.sdk.kotlin.e2etest.SqsTestUtils.getTestQueueUrl
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.internal.ValidationEnabled
import aws.sdk.kotlin.services.sqs.internal.ValidationScope
import aws.sdk.kotlin.services.sqs.model.*
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.hashing.md5
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*

/**
 * Tests for Sqs MD5 checksum validation
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqsMd5ChecksumValidationTest {
    // An interceptor that set wrong md5 checksums in SQS response
    private val wrongChecksumInterceptor = object : HttpInterceptor {
        override suspend fun modifyBeforeCompletion(
            context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>,
        ): Result<Any> {
            val wrongMd5ofMessageBody = "wrong message md5".encodeToByteArray().md5().toString()
            val wrongMd5ofMessageAttribute = "wrong attribute md5".encodeToByteArray().md5().toString()
            val wrongMd5ofMessageSystemAttribute = "wrong system attribute md5".encodeToByteArray().md5().toString()

            when (val response = context.response.getOrNull()) {
                is SendMessageResponse -> {
                    val modifiedResponse = SendMessageResponse.invoke {
                        messageId = response.messageId
                        sequenceNumber = response.sequenceNumber
                        md5OfMessageBody = wrongMd5ofMessageBody
                        md5OfMessageAttributes = wrongMd5ofMessageAttribute
                        md5OfMessageSystemAttributes = wrongMd5ofMessageSystemAttribute
                    }
                    println("modify SendMessage")
                    return Result.success(modifiedResponse)
                }
                is ReceiveMessageResponse -> {
                    val modifiedMessages = response.messages?.map { message ->
                        Message {
                            messageId = message.messageId
                            receiptHandle = message.receiptHandle
                            body = message.body
                            attributes = message.attributes
                            messageAttributes = message.messageAttributes
                            md5OfBody = wrongMd5ofMessageBody
                            md5OfMessageAttributes = wrongMd5ofMessageAttribute
                        }
                    }

                    val modifiedResponse = ReceiveMessageResponse {
                        messages = modifiedMessages
                    }
                    return Result.success(modifiedResponse)
                }
                is SendMessageBatchResponse -> {
                    val modifiedEntries = response.successful.map { entry ->
                        SendMessageBatchResultEntry {
                            id = entry.id
                            messageId = entry.messageId
                            md5OfMessageBody = wrongMd5ofMessageBody
                            md5OfMessageAttributes = wrongMd5ofMessageAttribute
                            md5OfMessageSystemAttributes = wrongMd5ofMessageSystemAttribute
                            sequenceNumber = entry.sequenceNumber
                        }
                    }

                    val modifiedResponse = SendMessageBatchResponse {
                        successful = modifiedEntries
                        failed = response.failed
                    }
                    return Result.success(modifiedResponse)
                }
            }
            return context.response
        }
    }

    private val correctChecksumClient = SqsClient {
        region = DEFAULT_REGION
        checksumValidationEnabled = ValidationEnabled.ALWAYS
        checksumValidationScopes = ValidationScope.entries.toSet()
    }

    // used for wrong checksum tests
    private val wrongChecksumClient = SqsClient {
        region = DEFAULT_REGION
        checksumValidationEnabled = ValidationEnabled.ALWAYS
        checksumValidationScopes = ValidationScope.entries.toSet()
        interceptors += wrongChecksumInterceptor
    }

    private lateinit var correctChecksumTestQueueUrl: String
    private lateinit var wrongChecksumTestQueueUrl: String

    @BeforeAll
    private fun setUp(): Unit = runBlocking {
        correctChecksumTestQueueUrl = getTestQueueUrl(correctChecksumClient, TEST_QUEUE_CORRECT_CHECKSUM_PREFIX, DEFAULT_REGION)
        wrongChecksumTestQueueUrl = getTestQueueUrl(wrongChecksumClient, TEST_QUEUE_WRONG_CHECKSUM_PREFIX, DEFAULT_REGION)
    }

    @AfterAll
    private fun cleanUp(): Unit = runBlocking {
        deleteQueueAndAllMessages(correctChecksumClient, correctChecksumTestQueueUrl)
        deleteQueueAndAllMessages(wrongChecksumClient, wrongChecksumTestQueueUrl)
        correctChecksumClient.close()
        wrongChecksumClient.close()
    }

    @Test
    fun testSendMessage(): Unit = runBlocking {
        assertDoesNotThrow {
            correctChecksumClient.sendMessage(
                SendMessageRequest {
                    queueUrl = correctChecksumTestQueueUrl
                    messageBody = TEST_MESSAGE_BODY
                    messageAttributes = hashMapOf(
                        TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                            dataType = "String"
                            stringValue = TEST_MESSAGE_ATTRIBUTES_VALUE
                        }
                    )
                    messageSystemAttributes = hashMapOf(
                        MessageSystemAttributeNameForSends.AwsTraceHeader to MessageSystemAttributeValue {
                            dataType = "String"
                            stringValue = TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE
                        }
                    )
                }
            )
        }
    }

    @Test
    fun testReceiveMessage(): Unit = runBlocking {
        assertDoesNotThrow {
            correctChecksumClient.receiveMessage(
                ReceiveMessageRequest {
                    queueUrl = correctChecksumTestQueueUrl
                    maxNumberOfMessages = 1
                    messageAttributeNames = listOf(TEST_MESSAGE_ATTRIBUTES_NAME)
                    messageSystemAttributeNames = listOf(MessageSystemAttributeName.AwsTraceHeader)
                }
            )
        }
    }

    @Test
    fun testSendMessageBatch(): Unit = runBlocking {
        val entries = (1..5).map { batchId ->
            buildSendMessageBatchRequestEntry(batchId)
        }

        assertDoesNotThrow {
            correctChecksumClient.sendMessageBatch(
                SendMessageBatchRequest {
                    queueUrl = correctChecksumTestQueueUrl
                    this.entries = entries
                }
            )
        }
    }

    @Test
    fun testSendMessageWithWrongChecksum(): Unit = runBlocking {
        val exception = assertThrows<ChecksumMismatchException> {
            wrongChecksumClient.sendMessage (
                SendMessageRequest {
                    queueUrl = wrongChecksumTestQueueUrl
                    messageBody = TEST_MESSAGE_BODY
                    messageAttributes = hashMapOf(
                        TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                            dataType = "String"
                            stringValue = TEST_MESSAGE_ATTRIBUTES_VALUE
                        }
                    )
                    messageSystemAttributes = hashMapOf(
                        MessageSystemAttributeNameForSends.AwsTraceHeader to MessageSystemAttributeValue {
                            dataType = "String"
                            stringValue = TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE
                        }
                    )
                }
            )
        }

        assert(exception.message!!.contains("Checksum mismatch"))
    }

    @Test
    fun testReceiveMessageWithWrongChecksum(): Unit = runBlocking {
        val exception = assertThrows<ChecksumMismatchException> {
            wrongChecksumClient.receiveMessage(
                ReceiveMessageRequest {
                    queueUrl = wrongChecksumTestQueueUrl
                    maxNumberOfMessages = 1
                    messageAttributeNames = listOf(TEST_MESSAGE_ATTRIBUTES_NAME)
                    messageSystemAttributeNames = listOf(MessageSystemAttributeName.AwsTraceHeader)
                }
            )
        }

        assert(exception.message!!.contains("Checksum mismatch"))
    }

    @Test
    fun testSendMessageBatchWithWrongChecksum(): Unit = runBlocking {
        val entries = (1..5).map { batchId ->
            buildSendMessageBatchRequestEntry(batchId)
        }

        val exception = assertThrows<ChecksumMismatchException> {
            wrongChecksumClient.sendMessageBatch(
                SendMessageBatchRequest {
                    queueUrl = wrongChecksumTestQueueUrl
                    this.entries = entries
                }
            )
        }

        assert(exception.message!!.contains("Checksum mismatch"))
    }
}
