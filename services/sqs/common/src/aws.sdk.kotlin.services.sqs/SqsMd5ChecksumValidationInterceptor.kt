/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.sqs

import aws.sdk.kotlin.services.sqs.internal.ValidationEnabled
import aws.sdk.kotlin.services.sqs.internal.ValidationScope
import aws.sdk.kotlin.services.sqs.model.*
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.hashing.Md5
import aws.smithy.kotlin.runtime.hashing.md5
import aws.smithy.kotlin.runtime.http.interceptors.ChecksumMismatchException
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.collections.hashMapOf
import kotlin.collections.isNullOrEmpty
import kotlin.collections.set
import kotlin.collections.sorted
import kotlin.collections.sortedBy

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
@OptIn(InternalApi::class, ExperimentalStdlibApi::class)
public class SqsMd5ChecksumValidationInterceptor(
    private val validationEnabled: ValidationEnabled?,
    private val validationScopes: Set<ValidationScope>,
) : HttpInterceptor {
    public companion object {
        private const val STRING_TYPE_FIELD_INDEX: Byte = 1
        private const val BINARY_TYPE_FIELD_INDEX: Byte = 2
        private const val STRING_LIST_TYPE_FIELD_INDEX: Byte = 3
        private const val BINARY_LIST_TYPE_FIELD_INDEX: Byte = 4

        private lateinit var logger: Logger

        private fun initLogger(logger: Logger) {
            this.logger = logger
        }
    }

    override fun readAfterExecution(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>) {
        val request = context.request
        val response = context.response.getOrNull()

        if (validationEnabled == ValidationEnabled.NEVER) return

        val logger = context.executionContext.coroutineContext.logger<SqsMd5ChecksumValidationInterceptor>()
        initLogger(logger)

        if (response != null) {
            when (request) {
                is SendMessageRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_RECEIVING) return

                    val sendMessageRequest = request as SendMessageRequest
                    val sendMessageResponse = response as SendMessageResponse
                    sendMessageOperationMd5Check(sendMessageRequest, sendMessageResponse)
                }

                is ReceiveMessageRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_SENDING) return

                    val receiveMessageResponse = response as ReceiveMessageResponse
                    receiveMessageResultMd5Check(receiveMessageResponse)
                }

                is SendMessageBatchRequest -> {
                    if (validationEnabled == ValidationEnabled.WHEN_RECEIVING) return

                    val sendMessageBatchRequest = request as SendMessageBatchRequest
                    val sendMessageBatchResponse = response as SendMessageBatchResponse
                    sendMessageBatchOperationMd5Check(sendMessageBatchRequest, sendMessageBatchResponse)
                }
            }
        }
    }

    private fun sendMessageOperationMd5Check(
        sendMessageRequest: SendMessageRequest,
        sendMessageResponse: SendMessageResponse,
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
            }
        }
    }

    private fun receiveMessageResultMd5Check(receiveMessageResponse: ReceiveMessageResponse) {
        val messages = receiveMessageResponse.messages
        if (messages != null) {
            for (messageReceived in messages) {
                if (validationScopes.contains(ValidationScope.MESSAGE_BODY)) {
                    val messageBody = messageReceived.body
                    if (!messageBody.isNullOrEmpty()) {
                        logger.debug { "Validating message body MD5 checksum for ReceiveMessage" }

                        val bodyMd5Returned = messageReceived.md5OfBody
                        val clientSideBodyMd5 = calculateMessageBodyMd5(messageBody)
                        if (clientSideBodyMd5 != bodyMd5Returned) {
                            throw ChecksumMismatchException("Checksum mismatch. Expected $clientSideBodyMd5 but was $bodyMd5Returned")
                        }
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
                    }
                }
            }
        }
    }

    private fun sendMessageBatchOperationMd5Check(
        sendMessageBatchRequest: SendMessageBatchRequest,
        sendMessageBatchResponse: SendMessageBatchResponse,
    ) {
        val idToRequestEntryMap = hashMapOf<String, SendMessageBatchRequestEntry>()
        val entries = sendMessageBatchRequest.entries
        if (entries != null) {
            for (entry in entries) {
                idToRequestEntryMap[entry.id] = entry
            }
        }

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
                }
            }
        }
    }

    private fun calculateMessageBodyMd5(messageBody: String): String {
        val expectedMD5 = try {
            messageBody.encodeToByteArray().md5()
        } catch (e: Exception) {
            throw ClientException(
                "Unable to calculate the MD5 hash of the message body." +
                    "Potential reasons include JVM configuration or FIPS compliance issues." +
                    "To disable message MD5 validation, you can set checksumValidationEnabled" +
                    "to false when instantiating the client." + e.message,
            )
        }
        val expectedMD5Hex = expectedMD5.toHexString()
        return expectedMD5Hex
    }

    /**
     * Calculates the MD5 digest for message attributes according to SQS specifications.
     * https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-message-metadata.html#sqs-attributes-md5-message-digest-calculation
     */
    @OptIn(InternalApi::class, ExperimentalStdlibApi::class)
    private fun calculateMessageAttributesMd5(messageAttributes: Map<String, MessageAttributeValue>): String {
        val sortedAttributeNames = messageAttributes.keys.sorted()
        val md5Digest = Md5()

        try {
            for (attributeName in sortedAttributeNames) {
                val attributeValue = messageAttributes[attributeName]
                updateLengthAndBytes(md5Digest, attributeName.encodeToByteArray())

                attributeValue?.dataType?.let { dataType ->
                    updateLengthAndBytes(md5Digest, dataType.encodeToByteArray())
                }

                val stringValue = attributeValue?.stringValue
                val binaryValue = attributeValue?.binaryValue
                val stringListValues = attributeValue?.stringListValues
                val binaryListValues = attributeValue?.binaryListValues

                when {
                    stringValue != null -> {
                        md5Digest.update(STRING_TYPE_FIELD_INDEX)
                        updateLengthAndBytes(md5Digest, stringValue.encodeToByteArray())
                    }

                    binaryValue != null -> {
                        md5Digest.update(BINARY_TYPE_FIELD_INDEX)
                        updateLengthAndBytes(md5Digest, binaryValue)
                    }

                    !stringListValues.isNullOrEmpty() -> {
                        md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX)
                        for (stringListValue in stringListValues) {
                            updateLengthAndBytes(md5Digest, stringListValue.encodeToByteArray())
                        }
                    }

                    !binaryListValues.isNullOrEmpty() -> {
                        md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX)
                        for (binaryListValue in binaryListValues) {
                            updateLengthAndBytes(md5Digest, binaryListValue)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw ClientException(
                "Unable to calculate the MD5 hash of the message body." +
                    "Potential reasons include JVM configuration or FIPS compliance issues." +
                    "To disable message MD5 validation, you can set checksumValidationEnabled" +
                    "to false when instantiating the client." + e.message,
            )
        }
        val expectedMD5Hex = md5Digest.digest().toHexString()
        return expectedMD5Hex
    }

    private fun calculateMessageSystemAttributesMd5(
        messageSysAttrs: Map<MessageSystemAttributeNameForSends, MessageSystemAttributeValue>,
    ): String {
        val sortedAttributeNames = messageSysAttrs.keys.sortedBy { it.value }
        val md5Digest = Md5()

        try {
            for (attributeName in sortedAttributeNames) {
                val attributeValue = messageSysAttrs[attributeName]
                updateLengthAndBytes(md5Digest, attributeName.value.encodeToByteArray())

                attributeValue?.dataType?.let { dataType ->
                    updateLengthAndBytes(md5Digest, dataType.encodeToByteArray())
                }

                val stringValue = attributeValue?.stringValue
                val binaryValue = attributeValue?.binaryValue
                val stringListValues = attributeValue?.stringListValues
                val binaryListValues = attributeValue?.binaryListValues

                when {
                    stringValue != null -> {
                        md5Digest.update(STRING_TYPE_FIELD_INDEX)
                        updateLengthAndBytes(md5Digest, stringValue.encodeToByteArray())
                    }

                    binaryValue != null -> {
                        md5Digest.update(BINARY_TYPE_FIELD_INDEX)
                        updateLengthAndBytes(md5Digest, binaryValue)
                    }

                    !stringListValues.isNullOrEmpty() -> {
                        md5Digest.update(STRING_LIST_TYPE_FIELD_INDEX)
                        for (stringListValue in stringListValues) {
                            updateLengthAndBytes(md5Digest, stringListValue.encodeToByteArray())
                        }
                    }

                    !binaryListValues.isNullOrEmpty() -> {
                        md5Digest.update(BINARY_LIST_TYPE_FIELD_INDEX)
                        for (binaryListValue in binaryListValues) {
                            updateLengthAndBytes(md5Digest, binaryListValue)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw ClientException(
                "Unable to calculate the MD5 hash of the message body." +
                    "Potential reasons include JVM configuration or FIPS compliance issues." +
                    "To disable message MD5 validation, you can set checksumValidationEnabled" +
                    "to false when instantiating the client." + e.message,
            )
        }
        val expectedMD5Hex = md5Digest.digest().toHexString()
        return expectedMD5Hex
    }

    /**
     * Update the digest using a sequence of bytes that consists of the length (in 4 bytes) of the
     * input binaryValue and all the bytes it contains.
     */
    private fun updateLengthAndBytes(messageDigest: Md5, binaryValue: ByteArray) {
        println("updateLengthAndBytes")
        val length = binaryValue.size
        val lengthBytes = byteArrayOf(
            (length shr 24).toByte(),
            (length shr 16).toByte(),
            (length shr 8).toByte(),
            length.toByte(),
        )

        messageDigest.update(lengthBytes)
        messageDigest.update(binaryValue)
    }
}
