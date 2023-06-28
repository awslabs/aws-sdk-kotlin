/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53.internal

import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.awsprotocol.ErrorDetails
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.xml.XmlDeserializer
import aws.smithy.kotlin.runtime.serde.xml.XmlSerialName

@InternalApi
public suspend fun parseCustomXmlErrorResponse(payload: ByteArray): ErrorDetails? {
    val details = InvalidChangeBatchDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: InvalidChangeBatchMessageDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: return null
    return ErrorDetails(details.code, details.message, details.requestId)
}

internal object InvalidChangeBatchDeserializer {
    private val MESSAGES_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("Messages"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("InvalidChangeBatch"))
        field(MESSAGES_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlErrorResponse? {
        var requestId: String? = null
        var messages: XmlError? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGES_DESCRIPTOR.index -> messages = InvalidChangeBatchMessageDeserializer.deserialize(deserializer)
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlErrorResponse(messages, requestId ?: messages?.requestId)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

internal object InvalidChangeBatchMessageDeserializer {
    private val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    private val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Messages"))
        field(MESSAGE_DESCRIPTOR)
        field(CODE_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlError? {
        var message: String? = null
        var code: String? = null
        var requestId: String? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                        CODE_DESCRIPTOR.index -> code = deserializeString()
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlError(requestId, code, message)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

// XML Error response parser from RestXMLErrorDeserializer
internal interface RestXmlErrorDetails {
    val requestId: String?
    val code: String?
    val message: String?
}

// Models "ErrorResponse" type in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
internal data class XmlErrorResponse(
    val error: XmlError?,
    override val requestId: String? = error?.requestId,
) : RestXmlErrorDetails {
    override val code: String? = error?.code
    override val message: String? = error?.message
}

// Models "Error" type in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
internal data class XmlError(
    override val requestId: String?,
    override val code: String?,
    override val message: String?,
) : RestXmlErrorDetails
