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
import aws.sdk.kotlin.e2etest.SqsTestUtils.TEST_QUEUE_PREFIX
import aws.sdk.kotlin.e2etest.SqsTestUtils.buildSendMessageBatchRequestEntry
import aws.sdk.kotlin.e2etest.SqsTestUtils.deleteQueueAndAllMessages
import aws.sdk.kotlin.e2etest.SqsTestUtils.getTestQueueUrl
import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.internal.ValidationEnabled
import aws.sdk.kotlin.services.sqs.model.*
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.hashing.md5
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * Tests for Sqs MD5 checksum validation
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SqsMd5ChecksumValidationTest {
    // An interceptor that set wrong md5 checksums in SQS response
    @OptIn(ExperimentalStdlibApi::class)
    private val wrongChecksumInterceptor = object : HttpInterceptor {
        override suspend fun modifyBeforeCompletion(
            context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>,
        ): Result<Any> {
            val wrongMd5ofMessageBody = "wrong message md5".encodeToByteArray().md5().toHexString()
            val wrongMd5ofMessageAttribute = "wrong attribute md5".encodeToByteArray().md5().toHexString()
            val wrongMd5ofMessageSystemAttribute = "wrong system attribute md5".encodeToByteArray().md5().toHexString()

            when (val response = context.response.getOrNull()) {
                is SendMessageResponse -> {
                    val modifiedResponse = response.copy {
                        md5OfMessageAttributes = wrongMd5ofMessageAttribute
                    }
                    return Result.success(modifiedResponse)
                }
                is ReceiveMessageResponse -> {
                    val modifiedMessages = response.messages?.map { message ->
                        message.copy {
                            md5OfBody = wrongMd5ofMessageBody
                        }
                    }

                    val modifiedResponse = ReceiveMessageResponse {
                        messages = modifiedMessages
                    }
                    return Result.success(modifiedResponse)
                }
                is SendMessageBatchResponse -> {
                    val modifiedEntries = response.successful.map { entry ->
                        entry.copy {
                            md5OfMessageSystemAttributes = wrongMd5ofMessageSystemAttribute
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

    // An interceptor that checks if the SQS md5 checksum was validated
    private val checksumValidationAssertionInterceptor = object : HttpInterceptor {
        private val supportedOperations = setOf(
            "SendMessage",
            "SendMessageBatch",
            "ReceiveMessage",
        )

        override fun readAfterExecution(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>) {
            val operationName = context.executionContext.attributes[AttributeKey("aws.smithy.kotlin#OperationName")] as String

            if (operationName !in supportedOperations) {
                return
            }

            assertNotNull(context.executionContext.attributes[AttributeKey("checksumValidated")])

            val isChecksumValidated = context.executionContext.attributes[AttributeKey("checksumValidated")] as Boolean

            assert(isChecksumValidated)
        }
    }

    private lateinit var correctChecksumClient: SqsClient

    private lateinit var wrongChecksumClient: SqsClient

    private lateinit var testQueueUrl: String

    @BeforeAll
    private fun setUp(): Unit = runBlocking {
        correctChecksumClient = SqsClient.fromEnvironment {
            region = DEFAULT_REGION
            checksumValidationEnabled = ValidationEnabled.ALWAYS
            interceptors += checksumValidationAssertionInterceptor
        }
        wrongChecksumClient = SqsClient.fromEnvironment {
            region = DEFAULT_REGION
            checksumValidationEnabled = ValidationEnabled.ALWAYS
            interceptors += wrongChecksumInterceptor
        }
        testQueueUrl = getTestQueueUrl(correctChecksumClient, TEST_QUEUE_PREFIX)
    }

    @AfterAll
    private fun cleanUp(): Unit = runBlocking {
        deleteQueueAndAllMessages(correctChecksumClient, testQueueUrl)
        correctChecksumClient.close()
        wrongChecksumClient.close()
    }

    @Test
    fun testSendMessage(): Unit = runBlocking {
        correctChecksumClient.sendMessage(
            SendMessageRequest {
                queueUrl = testQueueUrl
                messageBody = TEST_MESSAGE_BODY
                messageAttributes = mapOf(
                    TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                        dataType = "String"
                        stringValue = TEST_MESSAGE_ATTRIBUTES_VALUE
                    },
                    TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                        dataType = "Binary"
                        binaryValue = TEST_MESSAGE_ATTRIBUTES_VALUE.toByteArray()
                    },
                )
                messageSystemAttributes = mapOf(
                    MessageSystemAttributeNameForSends.AwsTraceHeader to MessageSystemAttributeValue {
                        dataType = "String"
                        stringValue = TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE
                    },
                )
            },
        )
    }

    @Test
    fun testReceiveMessage(): Unit = runBlocking {
        correctChecksumClient.receiveMessage(
            ReceiveMessageRequest {
                queueUrl = testQueueUrl
                maxNumberOfMessages = 1
                messageAttributeNames = listOf(TEST_MESSAGE_ATTRIBUTES_NAME)
                messageSystemAttributeNames = listOf(MessageSystemAttributeName.AwsTraceHeader)
            },
        )
    }

    @Test
    fun testSendMessageBatch(): Unit = runBlocking {
        val entries = (1..5).map { batchId ->
            buildSendMessageBatchRequestEntry(batchId)
        }

        correctChecksumClient.sendMessageBatch(
            SendMessageBatchRequest {
                queueUrl = testQueueUrl
                this.entries = entries
            },
        )
    }

    @Test
    fun testSendMessageWithWrongChecksum(): Unit = runBlocking {
        assertThrows<ChecksumMismatchException> {
            wrongChecksumClient.sendMessage(
                SendMessageRequest {
                    queueUrl = testQueueUrl
                    messageBody = TEST_MESSAGE_BODY
                    messageAttributes = mapOf(
                        TEST_MESSAGE_ATTRIBUTES_NAME to MessageAttributeValue {
                            dataType = "String"
                            stringValue = TEST_MESSAGE_ATTRIBUTES_VALUE
                        },
                    )
                    messageSystemAttributes = mapOf(
                        MessageSystemAttributeNameForSends.AwsTraceHeader to MessageSystemAttributeValue {
                            dataType = "String"
                            stringValue = TEST_MESSAGE_SYSTEM_ATTRIBUTES_VALUE
                        },
                    )
                },
            )
        }
    }

    @Test
    fun testReceiveMessageWithWrongChecksum(): Unit = runBlocking {
        assertThrows<ChecksumMismatchException> {
            wrongChecksumClient.receiveMessage(
                ReceiveMessageRequest {
                    queueUrl = testQueueUrl
                    maxNumberOfMessages = 1
                    messageAttributeNames = listOf(TEST_MESSAGE_ATTRIBUTES_NAME)
                    messageSystemAttributeNames = listOf(MessageSystemAttributeName.AwsTraceHeader)
                },
            )
        }
    }

    @Test
    fun testSendMessageBatchWithWrongChecksum(): Unit = runBlocking {
        val entries = (1..5).map { batchId ->
            buildSendMessageBatchRequestEntry(batchId)
        }

        assertThrows<ChecksumMismatchException> {
            wrongChecksumClient.sendMessageBatch(
                SendMessageBatchRequest {
                    queueUrl = testQueueUrl
                    this.entries = entries
                },
            )
        }
    }
}
