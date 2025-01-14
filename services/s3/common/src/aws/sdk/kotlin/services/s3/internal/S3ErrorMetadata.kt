/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.services.s3.model.S3ErrorMetadata
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.ClientErrorContext
import aws.smithy.kotlin.runtime.ErrorMetadata
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.awsprotocol.AwsErrorDetails
import aws.smithy.kotlin.runtime.awsprotocol.setAseErrorMetadata
import aws.smithy.kotlin.runtime.collections.appendValue
import aws.smithy.kotlin.runtime.collections.setIfValueNotNull
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.serde.xml.data
import aws.smithy.kotlin.runtime.serde.xml.xmlTagReader

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
        exception.sdkErrorMetadata.attributes.setIfValueNotNull(ServiceErrorMetadata.RequestId, errorDetails?.requestId)
    }

    if (exception is S3Exception) {
        (errorDetails?.requestId2 ?: response.headers[X_AMZN_REQUEST_ID_2_HEADER])?.let { requestId2 ->
            with(exception.sdkErrorMetadata) {
                attributes.setIfValueNotNull(S3ErrorMetadata.RequestId2, requestId2)

                attributes.appendValue(
                    ErrorMetadata.ClientContext,
                    ClientErrorContext("Extended request ID", requestId2),
                )
            }
        }
    }
}

internal fun parseS3ErrorResponse(payload: ByteArray): S3ErrorDetails {
    val root = xmlTagReader(payload)

    var message: String? = null
    var code: String? = null
    var requestId: String? = null
    var requestId2: String? = null

    loop@ while (true) {
        val curr = root.nextTag() ?: break@loop
        when (curr.tagName) {
            "Code" -> code = curr.data()
            "Message", "message" -> message = curr.data()
            "RequestId" -> requestId = curr.data()
            "HostId" -> requestId2 = curr.data()
        }
        curr.drop()
    }

    return S3ErrorDetails(code, message, requestId, requestId2)
}
