/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.retries

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.smithy.kotlin.runtime.retries.RetryDirective
import aws.smithy.kotlin.runtime.retries.RetryErrorType.*
import aws.smithy.kotlin.runtime.retries.impl.StandardRetryPolicy

public object AwsDefaultRetryPolicy : StandardRetryPolicy() {
    internal val knownErrorTypes = mapOf(
        "BandwidthLimitExceeded" to Throttling,
        "EC2ThrottledException" to Throttling,
        "IDPCommunicationError" to Timeout,
        "LimitExceededException" to Throttling,
        "PriorRequestNotComplete" to Throttling,
        "ProvisionedThroughputExceededException" to Throttling,
        "RequestLimitExceeded" to Throttling,
        "RequestThrottled" to Throttling,
        "RequestThrottledException" to Throttling,
        "RequestTimeout" to Timeout,
        "RequestTimeoutException" to Timeout,
        "SlowDown" to Throttling,
        "ThrottledException" to Throttling,
        "Throttling" to Throttling,
        "ThrottlingException" to Throttling,
        "TooManyRequestsException" to Throttling,
        "TransactionInProgressException" to Throttling,
    )

    override fun evaluateOtherExceptions(ex: Throwable): RetryDirective? = when (ex) {
        is AwsServiceException -> evaluateAwsServiceException(ex)
        else -> null
    }

    private fun evaluateAwsServiceException(ex: AwsServiceException): RetryDirective? = with(ex.sdkErrorMetadata) {
        knownErrorTypes[errorCode]?.let { RetryDirective.RetryError(it) }
    }
}
