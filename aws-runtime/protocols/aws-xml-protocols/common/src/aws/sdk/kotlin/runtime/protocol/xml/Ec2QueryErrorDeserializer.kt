/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.sdk.kotlin.runtime.http.middleware.errors.ErrorDetails
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.xml.XmlCollectionName
import aws.smithy.kotlin.runtime.serde.xml.XmlDeserializer
import aws.smithy.kotlin.runtime.serde.xml.XmlSerialName

internal data class Ec2QueryErrorResponse(val errors: List<Ec2QueryError>, val requestId: String?)

internal data class Ec2QueryError(val code: String?, val message: String?)

internal suspend fun parseEc2QueryErrorResponse(payload: ByteArray): ErrorDetails {
    val response = Ec2QueryErrorResponseDeserializer.deserialize(XmlDeserializer(payload, true))
    val firstError = response.errors.firstOrNull()
    return ErrorDetails(firstError?.code, firstError?.message, response.requestId)
}

/**
 * Deserializes EC2 Query protocol errors as specified by
 * https://awslabs.github.io/smithy/1.0/spec/aws/aws-ec2-query-protocol.html#operation-error-serialization
 */
internal object Ec2QueryErrorResponseDeserializer {
    private val ERRORS_DESCRIPTOR = SdkFieldDescriptor(
        SerialKind.List,
        XmlSerialName("Errors"),
        XmlCollectionName("Error"),
    )
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Response"))
        field(ERRORS_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): Ec2QueryErrorResponse {
        var errors = listOf<Ec2QueryError>()
        var requestId: String? = null

        deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
            loop@ while (true) {
                when (findNextFieldIndex()) {
                    ERRORS_DESCRIPTOR.index -> errors = deserializer.deserializeList(ERRORS_DESCRIPTOR) {
                        val collection = mutableListOf<Ec2QueryError>()
                        while (hasNextElement()) {
                            if (nextHasValue()) {
                                val element = Ec2QueryErrorDeserializer.deserialize(deserializer)
                                collection.add(element)
                            } else {
                                deserializeNull()
                                continue
                            }
                        }
                        collection
                    }
                    REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                    null -> break@loop
                    else -> skipValue()
                }
            }
        }

        return Ec2QueryErrorResponse(errors, requestId)
    }
}

internal object Ec2QueryErrorDeserializer {
    private val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
    private val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Error"))
        field(CODE_DESCRIPTOR)
        field(MESSAGE_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): Ec2QueryError {
        var code: String? = null
        var message: String? = null

        deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
            loop@ while (true) {
                when (findNextFieldIndex()) {
                    CODE_DESCRIPTOR.index -> code = deserializeString()
                    MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                    null -> break@loop
                    else -> skipValue()
                }
            }
        }

        return Ec2QueryError(code, message)
    }
}
