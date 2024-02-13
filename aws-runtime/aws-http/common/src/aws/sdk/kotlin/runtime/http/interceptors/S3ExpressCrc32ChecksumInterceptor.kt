/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.coroutines.coroutineContext

internal val S3_EXPRESS_ENDPOINT_PROPERTY_KEY = "backend"
internal val S3_EXPRESS_ENDPOINT_PROPERTY_VALUE = "S3Express"
private val CRC32_ALGORITHM_NAME = "CRC32"

public class S3ExpressCrc32ChecksumInterceptor(
    public val checksumAlgorithmHeaderName: String? = null,
) : HttpInterceptor {
    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        if (context.executionContext.getOrNull(AttributeKey(S3_EXPRESS_ENDPOINT_PROPERTY_KEY)) != S3_EXPRESS_ENDPOINT_PROPERTY_VALUE) {
            return context.protocolRequest
        }

        val logger = coroutineContext.logger<S3ExpressCrc32ChecksumInterceptor>()
        val req = context.protocolRequest.toBuilder()

        if (!context.executionContext.contains(HttpOperationContext.ChecksumAlgorithm)) {
            logger.info { "Checksum is required and not already configured, enabling CRC32 for S3 Express" }

            // Update the execution context so flexible checksums uses CRC32
            context.executionContext[HttpOperationContext.ChecksumAlgorithm] = CRC32_ALGORITHM_NAME

            // Most checksum headers are handled by the flexible checksums feature. But, S3 models an HTTP header binding for the
            // checksum algorithm, which also needs to be overwritten and set to CRC32.
            //
            // The header is already set by the time this interceptor runs, so it needs to be overwritten and can't be set
            // through the normal path.
            checksumAlgorithmHeaderName?.let {
                req.headers[it] = CRC32_ALGORITHM_NAME
            }
        }

        return req.build()
    }
}
