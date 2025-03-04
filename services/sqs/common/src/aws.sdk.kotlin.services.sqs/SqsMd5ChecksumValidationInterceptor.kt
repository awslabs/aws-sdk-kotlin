/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.sqs

import aws.sdk.kotlin.services.sqs.internal.ValidationEnabled
import aws.sdk.kotlin.services.sqs.internal.ValidationScope
import aws.sdk.kotlin.services.sqs.model.*
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.hashing.md5
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.logger

/**
 * Interceptor that validates MD5 checksums for SQS message operations.
 *
 * This interceptor performs client-side validation of MD5 checksums returned by SQS to ensure
 * message integrity during transmission. It validates the following components:
 * - Message body
 * - Message attributes
 * - Message system attributes
 *
 * The validation behavior can be configured using:
 * - [checksumValidationEnabled] - Controls when validation occurs (ALWAYS, WHEN_SENDING, WHEN_RECEIVING, NEVER)
 * - [checksumValidationScopes] - Specifies which message components to validate
 *
 * Supported operations:
 * - SendMessage
 * - SendMessageBatch
 * - ReceiveMessage
 */
@OptIn(ExperimentalStdlibApi::class)
public class SqsMd5ChecksumValidationInterceptor(
    private val validationEnabled: ValidationEnabled?,
    private val validationScopes: Set<ValidationScope>,
) : HttpInterceptor {
    public companion object {
        private const val STRING_TYPE_FIELD_INDEX: Byte = 1
        private const val BINARY_TYPE_FIELD_INDEX: Byte = 2
        private const val STRING_LIST_TYPE_FIELD_INDEX: Byte = 3
        private const val BINARY_LIST_TYPE_FIELD_INDEX: Byte = 4
    }

    override fun readAfterExecution(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>) {
        if (validationEnabled == ValidationEnabled.NEVER) return

        val logger = context.executionContext.coroutineContext.logger<SqsMd5ChecksumValidationInterceptor>()

        // Test MD5 availability
        try {
            "MD5".encodeToByteArray().md5()
        } catch (e: Exception) {
            logger.error { "MD5 checksums are not available (likely due to FIPS mode). Checksum validation will be disabled." }
            return
        }

        val request = context.request
        val response = context.response.getOrNull()

        if (response != null) {
            when (request) {
                is SendMessageRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_RECEIVING) return

                    val sendMessageRequest = request as SendMessageRequest
                    val sendMessageResponse = response as SendMessageResponse
                    sendMessageOperationMd5Check(sendMessageRequest, sendMessageResponse, logger)
                }

                is ReceiveMessageRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_SENDING) return

                    val receiveMessageResponse = response as ReceiveMessageResponse
                    receiveMessageResultMd5Check(receiveMessageResponse, logger)
                }

                is SendMessageBatchRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_RECEIVING) return

                    val sendMessageBatchRequest = request as SendMessageBatchRequest
                    val sendMessageBatchResponse = response as SendMessageBatchResponse
                    sendMessageBatchOperationMd5Check(sendMessageBatchRequest, sendMessageBatchResponse, logger)
                }
            }
        }
    }

    private fun sendMessageOperationMd5Check(
        sendMessageRequest: SendMessageRequest,
        sendMessageResponse: SendMessageResponse,
        logger: Logger
    ) {
        if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
            val messageBodySent = sendMessageRequest.messageBody

            if (!messageBodySent.isNullOrEmpty()) {
                logger.debug { "Validating message body MD5 checksum for SendMessage" }

                val bodyMD5Returned = sendMessageResponse.md5OfMessageBody
                val clientSideBodyMd5 = calculateMessageBodyMd5(messageBodySent)
                if (clientSideBodyMd5 != bodyMD5Returned) {
                    throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideBodyMd5 but was $bodyMD5Returned")
                }

                logger.debug { "Message body MD5 checksum for SendMessage validated" }
            }
        }

        if (validationScopes.contains(ValidationScope.MESSAGE_ATTRIBUTES)) {
            val messageAttrSent = sendMessageRequest.messageAttributes
            if (!messageAttrSent.isNullOrEmpty()) {
                logger.debug { "Validating message attribute MD5 checksum for SendMessage" }

                val messageAttrMD5Returned = sendMessageResponse.md5OfMessageAttributes
                val clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttrSent)
                if (clientSideAttrMd5 != messageAttrMD5Returned) {
                    throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideAttrMd5 but was $messageAttrMD5Returned")
                }

                logger.debug { "Message attribute MD5 checksum for SendMessage validated" }
            }
        }

        if (validationScopes.contains(ValidationScope.MESSAGE_SYSTEM_ATTRIBUTES)) {
            val messageSysAttrSent = sendMessageRequest.messageSystemAttributes
            if (!messageSysAttrSent.isNullOrEmpty()) {
                logger.debug { "Validating message system attribute MD5 checksum for SendMessage" }

                val messageSysAttrMD5Returned = sendMessageResponse.md5OfMessageSystemAttributes
                val clientSideSysAttrMd5 = calculateMessageSystemAttributesMd5(messageSysAttrSent)
                if (clientSideSysAttrMd5 != messageSysAttrMD5Returned) {
                    throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideSysAttrMd5 but was $messageSysAttrMD5Returned")
                }

                logger.debug { "Message system attribute MD5 checksum for SendMessage validated" }
            }
        }
    }

    private fun receiveMessageResultMd5Check(receiveMessageResponse: ReceiveMessageResponse, logger: Logger) {
        receiveMessageResponse.messages?.forEach { messageReceived ->
            if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
                val messageBody = messageReceived.body
                if (!messageBody.isNullOrEmpty()) {
                    logger.debug { "Validating message body MD5 checksum for ReceiveMessage" }

                    val bodyMd5Returned = messageReceived.md5OfBody
                    val clientSideBodyMd5 = calculateMessageBodyMd5(messageBody)
                    if (clientSideBodyMd5 != bodyMd5Returned) {
                        throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideBodyMd5 but was $bodyMd5Returned")
                    }

                    logger.debug { "Message body MD5 checksum for ReceiveMessage validated " }
                }
            }

            if (validationScopes.contains(ValidationScope.MESSAGE_ATTRIBUTES)) {
                val messageAttr = messageReceived.messageAttributes

                if (!messageAttr.isNullOrEmpty()) {
                    logger.debug { "Validating message attribute MD5 checksum for ReceiveMessage" }

                    val attrMd5Returned = messageReceived.md5OfMessageAttributes
                    val clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttr)
                    if (clientSideAttrMd5 != attrMd5Returned) {
                        throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideAttrMd5 but was $attrMd5Returned")
                    }

                    logger.debug { "Message attribute MD5 checksum for ReceiveMessage validated " }
                }
            }
        }
    }

    private fun sendMessageBatchOperationMd5Check(
        sendMessageBatchRequest: SendMessageBatchRequest,
        sendMessageBatchResponse: SendMessageBatchResponse,
        logger: Logger
    ) {
        val idToRequestEntryMap = sendMessageBatchRequest
            .entries
            .orEmpty()
            .associateBy { it.id }

        for (entry in sendMessageBatchResponse.successful) {
            if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
                val messageBody = idToRequestEntryMap[entry.id]?.messageBody

                if (!messageBody.isNullOrEmpty()) {
                    logger.debug { "Validating message body MD5 checksum for SendMessageBatch: ${entry.messageId}" }

                    val bodyMd5Returned = entry.md5OfMessageBody
                    val clientSideBodyMd5 = calculateMessageBodyMd5(messageBody)
                    if (clientSideBodyMd5 != bodyMd5Returned) {
                        throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideBodyMd5 but was $bodyMd5Returned")
                    }

                    logger.debug { "Message body MD5 checksum for SendMessageBatch: ${entry.messageId} validated" }
                }
            }

            if (validationScopes.contains(ValidationScope.MESSAGE_ATTRIBUTES)) {
                val messageAttrSent = idToRequestEntryMap[entry.id]?.messageAttributes
                if (!messageAttrSent.isNullOrEmpty()) {
                    logger.debug { "Validating message attribute MD5 checksum for SendMessageBatch: ${entry.messageId}" }

                    val messageAttrMD5Returned = entry.md5OfMessageAttributes
                    val clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttrSent)
                    if (clientSideAttrMd5 != messageAttrMD5Returned) {
                        throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideAttrMd5 but was $messageAttrMD5Returned")
                    }

                    logger.debug { "Message attribute MD5 checksum for SendMessageBatch: ${entry.messageId} validated" }
                }
            }

            if (validationScopes.contains(ValidationScope.MESSAGE_SYSTEM_ATTRIBUTES)) {
                val messageSysAttrSent = idToRequestEntryMap[entry.id]?.messageSystemAttributes
                if (!messageSysAttrSent.isNullOrEmpty()) {
                    logger.debug { "Validating message system attribute MD5 checksum for SendMessageBatch: ${entry.messageId}" }

                    val messageSysAttrMD5Returned = entry.md5OfMessageSystemAttributes
                    val clientSideSysAttrMd5 = calculateMessageSystemAttributesMd5(messageSysAttrSent)
                    if (clientSideSysAttrMd5 != messageSysAttrMD5Returned) {
                        throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideSysAttrMd5 but was $messageSysAttrMD5Returned")
                    }

                    logger.debug { "Message system attribute MD5 checksum for SendMessageBatch: ${entry.messageId} validated" }
                }
            }
        }
    }

    private fun calculateMessageBodyMd5(messageBody: String): String {
        val expectedMD5 = messageBody.encodeToByteArray().md5()
        val expectedMD5Hex = expectedMD5.toHexString()

        return expectedMD5Hex
    }

    /**
     * Calculates the MD5 digest for message attributes according to SQS specifications.
     * https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-message-metadata.html#sqs-attributes-md5-message-digest-calculation
     */
    private fun calculateMessageAttributesMd5(messageAttributes: Map<String, MessageAttributeValue>): String {
        val buffer = SdkBuffer()

        messageAttributes
            .entries
            .sortedBy { (name, _) -> name }
            .forEach { (attributeName, attributeValue) ->
                updateLengthAndBytes(buffer, attributeName)

                updateLengthAndBytes(buffer, attributeValue.dataType)

                val stringValue = attributeValue.stringValue
                val binaryValue = attributeValue.binaryValue
                val stringListValues = attributeValue.stringListValues
                val binaryListValues = attributeValue.binaryListValues

                when {
                    stringValue != null -> updateForStringType(buffer, stringValue)
                    binaryValue != null -> updateForBinaryType(buffer, binaryValue)
                    !stringListValues.isNullOrEmpty() -> updateForStringListType(buffer, stringListValues)
                    !binaryListValues.isNullOrEmpty() -> updateForBinaryListType(buffer, binaryListValues)
                }
        }

        val payload = buffer.readByteArray()
        return payload.md5().toHexString()
    }

    private fun calculateMessageSystemAttributesMd5(
        messageSysAttrs: Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue>,
    ): String {
        val buffer = SdkBuffer()

        messageSysAttrs
            .entries
            .sortedBy { (name, _) -> name.value }
            .forEach { (attributeName, attributeValue) ->
            updateLengthAndBytes(buffer, attributeName.value)

            updateLengthAndBytes(buffer, attributeValue.dataType)

            val stringValue = attributeValue.stringValue
            val binaryValue = attributeValue.binaryValue
            val stringListValues = attributeValue.stringListValues
            val binaryListValues = attributeValue.binaryListValues

            when {
                stringValue != null -> updateForStringType(buffer, stringValue)
                binaryValue != null -> updateForBinaryType(buffer, binaryValue)
                !stringListValues.isNullOrEmpty() -> updateForStringListType(buffer, stringListValues)
                !binaryListValues.isNullOrEmpty() -> updateForBinaryListType(buffer, binaryListValues)
            }
        }

        val payload = buffer.readByteArray()
        return payload.md5().toHexString()
    }

    private fun updateForStringType(buffer: SdkBuffer, value: String) {
        buffer.writeByte(STRING_TYPE_FIELD_INDEX)
        updateLengthAndBytes(buffer, value)
    }

    private fun updateForBinaryType(buffer: SdkBuffer, value: ByteArray) {
        buffer.writeByte(BINARY_TYPE_FIELD_INDEX)
        updateLengthAndBytes(buffer, value)
    }

    private fun updateForStringListType(buffer: SdkBuffer, values: List<String>) {
        buffer.writeByte(STRING_LIST_TYPE_FIELD_INDEX)
        values.forEach { value ->
            updateLengthAndBytes(buffer, value)
        }
    }

    private fun updateForBinaryListType(buffer: SdkBuffer, values: List<ByteArray>) {
        buffer.writeByte(BINARY_LIST_TYPE_FIELD_INDEX)
        values.forEach { value ->
            updateLengthAndBytes(buffer, value)
        }
    }

    private fun updateLengthAndBytes(buffer: SdkBuffer, stringValue: String) =
        updateLengthAndBytes(buffer, stringValue.encodeToByteArray())

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input binaryValue and all the bytes it contains.
     */
    private fun updateLengthAndBytes(buffer: SdkBuffer, binaryValue: ByteArray) {
        buffer.writeInt(binaryValue.size)
        buffer.write(binaryValue)
    }
}
