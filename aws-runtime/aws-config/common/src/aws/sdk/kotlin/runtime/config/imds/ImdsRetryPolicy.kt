/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.category
import aws.smithy.kotlin.runtime.retries.RetryDirective
import aws.smithy.kotlin.runtime.retries.RetryErrorType
import aws.smithy.kotlin.runtime.retries.RetryPolicy

internal class ImdsRetryPolicy : RetryPolicy<Any?> {
    override fun evaluate(result: Result<Any?>): RetryDirective = when {
        result.isSuccess -> RetryDirective.TerminateAndSucceed
        else -> evaluate(result.exceptionOrNull()!!)
    }

    private fun evaluate(throwable: Throwable): RetryDirective = when (throwable) {
        is EC2MetadataError -> {
            val status = HttpStatusCode.fromValue(throwable.statusCode)
            when {
                status.category() == HttpStatusCode.Category.SERVER_ERROR -> RetryDirective.RetryError(RetryErrorType.ServerSide)
                // 401 indicates the token has expired, this is retryable
                status == HttpStatusCode.Unauthorized -> RetryDirective.RetryError(RetryErrorType.ServerSide)
                else -> RetryDirective.TerminateAndFail
            }
        }
        else -> RetryDirective.TerminateAndFail
    }
}
