/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53.internal

import aws.sdk.kotlin.services.route53.model.InvalidChangeBatch
import aws.smithy.kotlin.runtime.awsprotocol.ErrorDetails
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.xml.XmlDeserializer
import aws.smithy.kotlin.runtime.serde.xml.XmlSerialName

internal suspend fun parseInvalidChangeBatchRestXmlErrorResponse(payload: ByteArray): InvalidChangeBatchErrorResponse? {
    val details = InvalidChangeBatchDeserializer.deserialize(XmlDeserializer(payload, true)) ?: return null
    val exception = buildInvalidChangeBatchException(details.messages)
    val errorDetails = ErrorDetails("InvalidChangeBatch", details.messages, details.requestId)
    return InvalidChangeBatchErrorResponse(errorDetails, exception)
}

private fun buildInvalidChangeBatchException(messages: String?): InvalidChangeBatch {
    messages ?: throw DeserializationException("Missing message in InvalidChangeBatch XML response")
    val builder = InvalidChangeBatch.Builder()
    builder.message = messages // TODO: Invalid change batch exception has no field for request ID
    return builder.build()
}

private object InvalidChangeBatchDeserializer {
    private val MESSAGES_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("Messages"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("InvalidChangeBatch"))
        field(MESSAGES_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): InvalidChangeBatchError? {
        var requestId: String? = null
        var messages: String? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGES_DESCRIPTOR.index -> messages = InvalidChangeBatchMessagesDeserializer.deserialize(deserializer)
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            InvalidChangeBatchError(messages, requestId)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

private object InvalidChangeBatchMessagesDeserializer {
    private val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Messages")) // TODO: The generic 'XmlErrorDeserializer' can't replace this one because this one is different
        field(MESSAGE_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): String? {
        var messages: String? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGE_DESCRIPTOR.index ->
                            if (messages == null) messages = deserializeString() else messages = messages + " + " + deserializeString() // TODO: What separator should I use?
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            messages // TODO: I looked at other deserializers and they only return a message, no request ID
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

internal data class InvalidChangeBatchErrorResponse(
    val errorDetails: ErrorDetails,
    val exception: InvalidChangeBatch,
)

private data class InvalidChangeBatchError(
    val messages: String?,
    val requestId: String?,
)
