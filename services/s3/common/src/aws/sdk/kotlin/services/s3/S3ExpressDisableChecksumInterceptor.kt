/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.coroutines.coroutineContext

/**
 * Disable checksums entirely for s3:UploadPart requests.
 */
public class S3ExpressDisableChecksumInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        if (context.executionContext.getOrNull(AttributeKey(S3_EXPRESS_ENDPOINT_PROPERTY_KEY)) != S3_EXPRESS_ENDPOINT_PROPERTY_VALUE) {
            return context.protocolRequest
        }

        val logger = coroutineContext.logger<S3ExpressDisableChecksumInterceptor>()

        val configuredChecksumAlgorithm = context.executionContext.getOrNull(HttpOperationContext.ChecksumAlgorithm)

        configuredChecksumAlgorithm?.let {
            logger.warn { "Disabling configured checksum $it for S3 Express UploadPart" }
            context.executionContext.remove(HttpOperationContext.ChecksumAlgorithm)
        }

        return context.protocolRequest
    }
}
