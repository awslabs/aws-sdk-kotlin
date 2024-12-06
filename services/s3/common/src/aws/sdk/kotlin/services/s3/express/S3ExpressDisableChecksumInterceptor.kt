/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.express

import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.http.DeferredHeadersBuilder
import aws.smithy.kotlin.runtime.http.HeadersBuilder
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.coroutines.coroutineContext

private const val CHECKSUM_HEADER_PREFIX = "x-amz-checksum-"

/**
 * Disables checksums for s3:UploadPart requests that use S3 express.
 */
internal class S3ExpressDisableChecksumInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeSigning(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        if (context.executionContext.getOrNull(AttributeKey(S3_EXPRESS_ENDPOINT_PROPERTY_KEY)) != S3_EXPRESS_ENDPOINT_PROPERTY_VALUE) {
            return context.protocolRequest
        }

        val logger = coroutineContext.logger<S3ExpressDisableChecksumInterceptor>()
        logger.warn { "Checksums must not be sent with S3 express upload part operation, removing checksum(s)" }

        val request = context.protocolRequest.toBuilder()

        request.headers.removeChecksumHeaders()
        request.trailingHeaders.removeChecksumTrailingHeaders()
        request.headers.removeChecksumTrailingHeadersFromXAmzTrailer()

        return request.build()
    }
}

/**
 * Removes any checksums sent in the request's headers
 */
internal fun HeadersBuilder.removeChecksumHeaders(): Unit =
    names().forEach { name ->
        if (name.startsWith(CHECKSUM_HEADER_PREFIX)) {
            remove(name)
        }
    }

/**
 * Removes any checksums sent in the request's trailing headers
 */
internal fun DeferredHeadersBuilder.removeChecksumTrailingHeaders(): Unit =
    names().forEach { name ->
        if (name.startsWith(CHECKSUM_HEADER_PREFIX)) {
            remove(name)
        }
    }

/**
 * Removes any checksums sent in the request's trailing headers from `x-amz-trailer`
 */
internal fun HeadersBuilder.removeChecksumTrailingHeadersFromXAmzTrailer() {
    this.getAll("x-amz-trailer")?.forEach { trailingHeader ->
        if (trailingHeader.startsWith(CHECKSUM_HEADER_PREFIX)) {
            this.remove("x-amz-trailer", trailingHeader)
        }
    }
}
