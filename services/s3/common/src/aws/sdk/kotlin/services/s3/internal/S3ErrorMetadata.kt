/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.*
import aws.sdk.kotlin.runtime.http.*
import aws.sdk.kotlin.services.s3.model.S3ErrorMetadata
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.xml.XmlDeserializer
import aws.smithy.kotlin.runtime.serde.xml.XmlSerialName
import aws.smithy.kotlin.runtime.util.setIfValueNotNull

/**
 * Default header name identifying secondary request ID
 * See https://aws.amazon.com/premiumsupport/knowledge-center/s3-request-id-values
 */
private const val X_AMZN_REQUEST_ID_2_HEADER: String = "x-amz-id-2"

internal data class S3ErrorDetails(
    override val code: String? = null,
    override val message: String? = null,
    override val requestId: String? = null,
    val requestId2: String? = null,
) : AwsErrorDetails

/**
 * Pull specific details from the response / error and set [S3Exception] metadata
 */
internal fun setS3ErrorMetadata(exception: Any, response: HttpResponse, errorDetails: S3ErrorDetails?) {
    setAseErrorMetadata(exception, response, errorDetails)
    if (exception is AwsServiceException) {
        exception.sdkErrorMetadata.attributes.setIfValueNotNull(AwsErrorMetadata.RequestId, errorDetails?.requestId)
    }
    if (exception is S3Exception) {
        val requestId2 = errorDetails?.requestId2 ?: response.headers[X_AMZN_REQUEST_ID_2_HEADER]
        exception.sdkErrorMetadata.attributes.setIfValueNotNull(S3ErrorMetadata.RequestId2, requestId2)
    }
}

internal suspend fun parseS3ErrorResponse(payload: ByteArray): S3ErrorDetails {
    val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
    val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    val HOSTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("HostId"))
    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Error"))
        field(MESSAGE_DESCRIPTOR)
        field(CODE_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
        field(HOSTID_DESCRIPTOR)
    }

    var message: String? = null
    var code: String? = null
    var requestId: String? = null
    var requestId2: String? = null

    val deserializer = XmlDeserializer(payload, true)
    deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
        loop@ while (true) {
            when (findNextFieldIndex()) {
                MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                CODE_DESCRIPTOR.index -> code = deserializeString()
                REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                HOSTID_DESCRIPTOR.index -> requestId2 = deserializeString()
                null -> break@loop
                else -> skipValue()
            }
        }
    }

    return S3ErrorDetails(code, message, requestId, requestId2)
}
