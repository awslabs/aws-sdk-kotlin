/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk.restjson

import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.serde.*
import software.aws.clientrt.serde.json.JsonDeserializer

// header identifying the error code
const val X_AMZN_ERROR_TYPE_HEADER_NAME = "X-Amzn-Errortype"

// returned by RESTFUL services that do no send a payload (like in a HEAD request)
const val X_AMZN_ERROR_MESSAGE_HEADER_NAME = "x-amzn-error-message"

// error message header returned by event stream errors
const val X_AMZN_EVENT_ERROR_MESSAGE_HEADER_NAME = ":error-message"

internal data class RestJsonErrorDetails(val code: String? = null, val message: String? = null)

/**
 * Deserializes rest JSON protocol errors as specified by:
 *     - Smithy spec: https://awslabs.github.io/smithy/1.0/spec/aws/aws-restjson1-protocol.html#operation-error-serialization
 *     - SDK Unmarshal Service API Errors (SEP)
 *     - x-amzn-ErrorMessage (SEP)
 */
internal object RestJsonErrorDeserializer {
    // alternative field descriptors for error codes embedded in the document
    private val ERR_CODE_ALT1_DESCRIPTOR = SdkFieldDescriptor("code", SerialKind.Integer)
    private val ERR_CODE_ALT2_DESCRIPTOR = SdkFieldDescriptor("__type", SerialKind.Integer)

    // alternative field descriptors for the error message embedded in the document
    private val MESSAGE_ALT1_DESCRIPTOR = SdkFieldDescriptor("message", SerialKind.String)
    private val MESSAGE_ALT2_DESCRIPTOR = SdkFieldDescriptor("Message", SerialKind.String)
    private val MESSAGE_ALT3_DESCRIPTOR = SdkFieldDescriptor("errorMessage", SerialKind.String)

    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        field(ERR_CODE_ALT1_DESCRIPTOR)
        field(ERR_CODE_ALT2_DESCRIPTOR)
        field(MESSAGE_ALT1_DESCRIPTOR)
        field(MESSAGE_ALT2_DESCRIPTOR)
        field(MESSAGE_ALT3_DESCRIPTOR)
    }

    fun deserialize(response: HttpResponse, payload: ByteArray?): RestJsonErrorDetails {
        var code: String? = response.headers[X_AMZN_ERROR_TYPE_HEADER_NAME]
        var message: String? = response.headers[X_AMZN_ERROR_MESSAGE_HEADER_NAME]
        if (message == null) {
            message = response.headers[X_AMZN_EVENT_ERROR_MESSAGE_HEADER_NAME]
        }

        if (payload != null) {
            val deserializer = JsonDeserializer(payload)
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@while (true) {
                    when (findNextFieldIndex()) {
                        ERR_CODE_ALT1_DESCRIPTOR.index,
                        ERR_CODE_ALT2_DESCRIPTOR.index -> code = deserializeString()
                        MESSAGE_ALT1_DESCRIPTOR.index,
                        MESSAGE_ALT2_DESCRIPTOR.index,
                        MESSAGE_ALT3_DESCRIPTOR.index -> message = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
        }

        return RestJsonErrorDetails(sanitize(code), message)
    }
}

/**
 * Sanitize the value to retrieve the disambiguated error type using the following steps:
 *
 * If a : character is present, then take only the contents before the first : character in the value.
 * If a # character is present, then take only the contents after the first # character in the value.
 *
 * All of the following error values resolve to FooError:
 *
 * FooError
 * FooError:http://amazon.com/smithy/com.amazon.smithy.validate/
 * aws.protocoltests.restjson#FooError
 * aws.protocoltests.restjson#FooError:http://amazon.com/smithy/com.amazon.smithy.validate/
 */
private fun sanitize(code: String?): String? {
    if (code == null) return code
    return code.substringAfter("#")
        .substringBefore(":")
}
