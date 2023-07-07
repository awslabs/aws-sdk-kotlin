/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53.internal

import aws.sdk.kotlin.services.route53.model.InvalidChangeBatch
import aws.smithy.kotlin.runtime.awsprotocol.ErrorDetails
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.xml.*

internal fun parseRestXmlInvalidChangeBatchResponse(payload: ByteArray): InvalidChangeBatchErrorResponse? {
    return deserializeInvalidChangeBatchError(InvalidChangeBatch.Builder(), payload)
}

internal fun deserializeInvalidChangeBatchError(builder: InvalidChangeBatch.Builder, payload: ByteArray): InvalidChangeBatchErrorResponse? {
    val deserializer = XmlDeserializer(payload)
    val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("message"), XmlAliasName("Message"))
    val MESSAGES_DESCRIPTOR = SdkFieldDescriptor(SerialKind.List, XmlSerialName("messages"), XmlAliasName("Messages"), XmlCollectionName("Message"))
    val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("InvalidChangeBatch"))
        trait(XmlNamespace("https://route53.amazonaws.com/doc/2013-04-01/"))
        field(MESSAGE_DESCRIPTOR)
        field(MESSAGES_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }
    var requestId: String? = null

    return try {
        deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
            loop@while (true) {
                when (findNextFieldIndex()) {
                    MESSAGE_DESCRIPTOR.index -> builder.message = deserializeString()
                    REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                    MESSAGES_DESCRIPTOR.index ->
                        builder.messages = deserializer.deserializeList(MESSAGES_DESCRIPTOR) {
                            val col0 = mutableListOf<String>()
                            while (hasNextElement()) {
                                val el0 = if (nextHasValue()) { deserializeString() } else { deserializeNull(); continue }
                                col0.add(el0)
                            }
                            col0
                        }
                    null -> break@loop
                    else -> skipValue()
                }
            }
        }
        InvalidChangeBatchErrorResponse(ErrorDetails("InvalidChangeBatch", builder.message, requestId), builder.build())
    } catch (e: DeserializationException) {
        null // return so an appropriate exception type can be instantiated above here.
    }
}

internal data class InvalidChangeBatchErrorResponse(
    val errorDetails: ErrorDetails,
    val exception: InvalidChangeBatch,
)
