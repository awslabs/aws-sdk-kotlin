/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.AwsErrorMetadata
import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.util.setIfNotNull

/**
 * Common error response details
 */
@InternalSdkApi
public data class ErrorDetails(val code: String?, val message: String?, val requestId: String?)

/**
 * Pull specific details from the response / error and set [AwsServiceException] metadata
 */
@InternalSdkApi
public fun setAseErrorMetadata(exception: Any, response: HttpResponse, errorDetails: ErrorDetails?) {
    if (exception is AwsServiceException) {
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorCode, errorDetails?.code)
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorMessage, errorDetails?.message)
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, response.headers[X_AMZN_REQUEST_ID_HEADER])
        exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
    }
}
