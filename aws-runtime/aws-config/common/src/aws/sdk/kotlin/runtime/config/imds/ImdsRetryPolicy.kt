/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.imds

import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.category
import aws.smithy.kotlin.runtime.retries.policy.RetryDirective
import aws.smithy.kotlin.runtime.retries.policy.RetryErrorType
import aws.smithy.kotlin.runtime.retries.policy.StandardRetryPolicy
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.coroutines.CoroutineContext

internal class ImdsRetryPolicy(private val callContext: CoroutineContext) : StandardRetryPolicy() {
    override fun evaluateSpecificExceptions(ex: Throwable) = when (ex) {
        is EC2MetadataError -> {
            val status = ex.status
            when {
                status.category() == HttpStatusCode.Category.SERVER_ERROR -> RetryDirective.RetryError(RetryErrorType.ServerSide)
                // 401 indicates the token has expired, this is retryable
                status == HttpStatusCode.Unauthorized -> RetryDirective.RetryError(RetryErrorType.ServerSide)
                else -> {
                    val logger = callContext.logger<ImdsRetryPolicy>()
                    logger.debug { "IMDS error: statusCode=$status; ${ex.message}" }
                    null
                }
            }
        }
        else -> null
    }
}
