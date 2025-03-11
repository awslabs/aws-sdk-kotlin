/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.sqs

import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.services.sqs.internal.ValidationEnabled
import aws.sdk.kotlin.services.sqs.internal.ValidationScope
import aws.sdk.kotlin.services.sqs.model.*
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.hashing.md5
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.error
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import aws.smithy.kotlin.runtime.util.asyncLazy
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

private const val STRING_TYPE_FIELD_INDEX: Byte = 1
private const val BINARY_TYPE_FIELD_INDEX: Byte = 2
private const val STRING_LIST_TYPE_FIELD_INDEX: Byte = 3
private const val BINARY_LIST_TYPE_FIELD_INDEX: Byte = 4

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
 * - [checksumValidationEnabled] - Controls when validation occurs (`ALWAYS`, `WHEN_SENDING`, `WHEN_RECEIVING`, `NEVER`)
 * - [checksumValidationScopes] - Specifies which message components to validate
 *
 * Supported operations:
 * - SendMessage
 * - SendMessageBatch
 * - ReceiveMessage
 */
@OptIn(ExperimentalStdlibApi::class)
public class SqsMd5ChecksumValidationInterceptor(
    private val validationEnabled: ValidationEnabled,
    private val validationScopes: Set<ValidationScope>,
) : HttpInterceptor {
    public companion object {
        private val isMd5Available = asyncLazy {
            try {
                "MD5".encodeToByteArray().md5()
                true
            } catch (e: Exception) {
                coroutineContext.error<SqsMd5ChecksumValidationInterceptor>(e) {
                    "MD5 checksums are not available (likely due to FIPS mode). Checksum validation will be disabled."
                }
                false
            }
        }
    }

    override fun readAfterExecution(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>) {
        if (validationEnabled == ValidationEnabled.NEVER || runBlocking { !isMd5Available.get() }) return

        val logger = context.executionContext.coroutineContext.logger<SqsMd5ChecksumValidationInterceptor>()

        val request = context.request

        context.response.getOrNull()?.let { response ->
            when (request) {
                is SendMessageRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_RECEIVING) return

                    val sendMessageResponse = response as SendMessageResponse
                    sendMessageOperationMd5Check(request, sendMessageResponse, logger)
                }

                is ReceiveMessageRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_SENDING) return

                    val receiveMessageResponse = response as ReceiveMessageResponse
                    receiveMessageResultMd5Check(receiveMessageResponse, logger)
                }

                is SendMessageBatchRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_RECEIVING) return

                    val sendMessageBatchResponse = response as SendMessageBatchResponse
                    sendMessageBatchOperationMd5Check(request, sendMessageBatchResponse, logger)
                }
            }
        }

        // Sets validation flag in execution context for e2e test assertions
        val checksumValidated: AttributeKey<Boolean> = AttributeKey("checksumValidated")
        context.executionContext[checksumValidated] = true
    }

    private fun sendMessageOperationMd5Check(
        sendMessageRequest: SendMessageRequest,
        sendMessageResponse: SendMessageResponse,
        logger: Logger,
    ) {
        if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
            val messageBodyMd5Returned = sendMessageResponse.md5OfMessageBody
            val messageBodySent = sendMessageRequest.messageBody

            if (!messageBodyMd5Returned.isNullOrEmpty() && !messageBodySent.isNullOrEmpty()) {
                logger.debug { "Validating message body MD5 checksum for SendMessage" }

                val clientSideBodyMd5 = calculateMessageBodyMd5(messageBodySent)

                validateMd5(clientSideBodyMd5, messageBodyMd5Returned)

                logger.debug { "Message body MD5 checksum for SendMessage validated" }
            }
        }

        if (validationScopes.contains(ValidationScope.MESSAGE_ATTRIBUTES)) {
            val messageAttrMd5Returned = sendMessageResponse.md5OfMessageAttributes
            val messageAttrSent = sendMessageRequest.messageAttributes

            if (!messageAttrMd5Returned.isNullOrEmpty() && !messageAttrSent.isNullOrEmpty()) {
                logger.debug { "Validating message attribute MD5 checksum for SendMessage" }

                val clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttrSent)

                validateMd5(clientSideAttrMd5, messageAttrMd5Returned)

                logger.debug { "Message attribute MD5 checksum for SendMessage validated" }
            }
        }

        if (validationScopes.contains(ValidationScope.MESSAGE_SYSTEM_ATTRIBUTES)) {
            val messageSysAttrMD5Returned = sendMessageResponse.md5OfMessageSystemAttributes
            val messageSysAttrSent = sendMessageRequest.messageSystemAttributes

            if (!messageSysAttrMD5Returned.isNullOrEmpty() && !messageSysAttrSent.isNullOrEmpty()) {
                logger.debug { "Validating message system attribute MD5 checksum for SendMessage" }

                val clientSideSysAttrMd5 = calculateMessageSystemAttributesMd5(messageSysAttrSent)

                validateMd5(clientSideSysAttrMd5, messageSysAttrMD5Returned)

                logger.debug { "Message system attribute MD5 checksum for SendMessage validated" }
            }
        }
    }

    private fun receiveMessageResultMd5Check(receiveMessageResponse: ReceiveMessageResponse, logger: Logger) {
        receiveMessageResponse.messages?.forEach { messageReceived ->
            if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
                val messageBodyMd5Returned = messageReceived.md5OfBody
                val messageBodyReturned = messageReceived.body

                if (!messageBodyMd5Returned.isNullOrEmpty() && !messageBodyReturned.isNullOrEmpty()) {
                    logger.debug { "Validating message body MD5 checksum for ReceiveMessage" }

                    val clientSideBodyMd5 = calculateMessageBodyMd5(messageBodyReturned)

                    validateMd5(clientSideBodyMd5, messageBodyMd5Returned)

                    logger.debug { "Message body MD5 checksum for ReceiveMessage validated " }
                }
            }

            if (validationScopes.contains(ValidationScope.MESSAGE_ATTRIBUTES)) {
                val messageAttrMd5Returned = messageReceived.md5OfMessageAttributes
                val messageAttrReturned = messageReceived.messageAttributes

                if (!messageAttrMd5Returned.isNullOrEmpty() && !messageAttrReturned.isNullOrEmpty()) {
                    logger.debug { "Validating message attribute MD5 checksum for ReceiveMessage" }

                    val clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttrReturned)

                    validateMd5(clientSideAttrMd5, messageAttrMd5Returned)

                    logger.debug { "Message attribute MD5 checksum for ReceiveMessage validated " }
                }
            }
        }
    }

    private fun sendMessageBatchOperationMd5Check(
        sendMessageBatchRequest: SendMessageBatchRequest,
        sendMessageBatchResponse: SendMessageBatchResponse,
        logger: Logger,
    ) {
        val idToRequestEntry = sendMessageBatchRequest
            .entries
            .orEmpty()
            .associateBy { it.id }

        for (entry in sendMessageBatchResponse.successful) {
            if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
                val messageBodyMd5Returned = entry.md5OfMessageBody
                val messageBodySent = idToRequestEntry[entry.id]?.messageBody

                if (!messageBodyMd5Returned.isNullOrEmpty() && !messageBodySent.isNullOrEmpty()) {
                    logger.debug { "Validating message body MD5 checksum for SendMessageBatch: ${entry.messageId}" }

                    val clientSideBodyMd5 = calculateMessageBodyMd5(messageBodySent)

                    validateMd5(clientSideBodyMd5, messageBodyMd5Returned)

                    logger.debug { "Message body MD5 checksum for SendMessageBatch: ${entry.messageId} validated" }
                }
            }

            if (validationScopes.contains(ValidationScope.MESSAGE_ATTRIBUTES)) {
                val messageAttrMD5Returned = entry.md5OfMessageAttributes
                val messageAttrSent = idToRequestEntry[entry.id]?.messageAttributes

                if (!messageAttrMD5Returned.isNullOrEmpty() && !messageAttrSent.isNullOrEmpty()) {
                    logger.debug { "Validating message attribute MD5 checksum for SendMessageBatch: ${entry.messageId}" }

                    val clientSideAttrMd5 = calculateMessageAttributesMd5(messageAttrSent)

                    validateMd5(clientSideAttrMd5, messageAttrMD5Returned)

                    logger.debug { "Message attribute MD5 checksum for SendMessageBatch: ${entry.messageId} validated" }
                }
            }

            if (validationScopes.contains(ValidationScope.MESSAGE_SYSTEM_ATTRIBUTES)) {
                val messageSysAttrMD5Returned = entry.md5OfMessageSystemAttributes
                val messageSysAttrSent = idToRequestEntry[entry.id]?.messageSystemAttributes

                if (!messageSysAttrMD5Returned.isNullOrEmpty() && !messageSysAttrSent.isNullOrEmpty()) {
                    logger.debug { "Validating message system attribute MD5 checksum for SendMessageBatch: ${entry.messageId}" }

                    val clientSideSysAttrMd5 = calculateMessageSystemAttributesMd5(messageSysAttrSent)

                    validateMd5(clientSideSysAttrMd5, messageSysAttrMD5Returned)

                    logger.debug { "Message system attribute MD5 checksum for SendMessageBatch: ${entry.messageId} validated" }
                }
            }
        }
    }

    private fun validateMd5(clientSideMd5: String, md5Returned: String) {
        if (clientSideMd5 != md5Returned) {
            throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideMd5 but was $md5Returned")
        }
    }

    private fun calculateMessageBodyMd5(messageBody: String) =
        messageBody.encodeToByteArray().md5().toHexString()

    /**
     * Calculates the MD5 digest for message attributes according to SQS specifications.
     * https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-message-metadata.html#sqs-attributes-md5-message-digest-calculation
     */
    private fun calculateMessageAttributesMd5(messageAttributes: Map<String, MessageAttributeValue>): String {
        val buffer = SdkBuffer()

        messageAttributes
            .entries
            .sortedBy { (attributeName, _) -> attributeName }
            .forEach { (attributeName, attributeValue) ->
                updateLengthAndBytes(buffer, attributeName)
                updateLengthAndBytes(buffer, attributeValue.dataType)
                when {
                    attributeValue.stringValue != null -> updateForStringType(buffer, attributeValue.stringValue)
                    attributeValue.binaryValue != null -> updateForBinaryType(buffer, attributeValue.binaryValue)
                    !attributeValue.stringListValues.isNullOrEmpty() -> updateForStringListType(buffer, attributeValue.stringListValues)
                    !attributeValue.binaryListValues.isNullOrEmpty() -> updateForBinaryListType(buffer, attributeValue.binaryListValues)
                    else -> throw ClientException("No value type found for attribute $attributeName")
                }
            }

        val payload = buffer.readByteArray()
        return payload.md5().toHexString()
    }

    private fun calculateMessageSystemAttributesMd5(
        messageSystemAttributes: Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue>,
    ): String {
        val buffer = SdkBuffer()

        messageSystemAttributes
            .entries
            .sortedBy { (systemAttributeName, _) -> systemAttributeName.value }
            .forEach { (systemAttributeName, systemAttributeValue) ->
                updateLengthAndBytes(buffer, systemAttributeName.value)
                updateLengthAndBytes(buffer, systemAttributeValue.dataType)
                when {
                    systemAttributeValue.stringValue != null -> updateForStringType(buffer, systemAttributeValue.stringValue)
                    systemAttributeValue.binaryValue != null -> updateForBinaryType(buffer, systemAttributeValue.binaryValue)
                    !systemAttributeValue.stringListValues.isNullOrEmpty() -> updateForStringListType(buffer, systemAttributeValue.stringListValues)
                    !systemAttributeValue.binaryListValues.isNullOrEmpty() -> updateForBinaryListType(buffer, systemAttributeValue.binaryListValues)
                    else -> throw ClientException("No value type found for system attribute $systemAttributeName")
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
