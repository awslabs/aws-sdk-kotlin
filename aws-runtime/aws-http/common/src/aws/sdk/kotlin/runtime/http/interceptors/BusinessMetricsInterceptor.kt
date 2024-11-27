/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.http.BUSINESS_METRICS_MAX_LENGTH
import aws.sdk.kotlin.runtime.http.middleware.USER_AGENT
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.telemetry.logging.Logger
import aws.smithy.kotlin.runtime.telemetry.logging.logger
import kotlin.coroutines.coroutineContext

/**
 * Appends business metrics to the `User-Agent` header.
 */
public class BusinessMetricsInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        val logger = coroutineContext.logger<BusinessMetricsInterceptor>()

        context.executionContext.getOrNull(BusinessMetrics)?.let { metrics ->
            val metricsString = formatMetrics(metrics, logger)
            val currentUserAgentHeader = context.protocolRequest.headers[USER_AGENT]
            val modifiedRequest = context.protocolRequest.toBuilder()

            modifiedRequest.headers[USER_AGENT] = currentUserAgentHeader + metricsString

            return modifiedRequest.build()
        }
        return context.protocolRequest
    }
}

/**
 * Makes sure the metrics do not exceed the maximum size and truncates them if so.
 * Makes sure that metric identifiers are not > 2 chars in length. Skips them if so.
 */
private fun formatMetrics(metrics: MutableSet<BusinessMetric>, logger: Logger): String {
    val allowedMetrics = metrics.filter {
        if (it.identifier.length > 2) {
            logger.warn { "Business metric '${it.identifier}' will be skipped due to length being > 2" }
            false
        } else {
            true
        }
    }
    if (allowedMetrics.isEmpty()) return ""
    val metricsString = allowedMetrics.joinToString(",", "m/") { it.identifier }
    val metricsByteArray = metricsString.encodeToByteArray()

    if (metricsByteArray.size <= BUSINESS_METRICS_MAX_LENGTH) return metricsString

    val lastCommaIndex = metricsByteArray
        .sliceArray(0 until 1024)
        .indexOfLast { it == ','.code.toByte() }
        .takeIf { it != -1 }

    lastCommaIndex?.let {
        return metricsByteArray.decodeToString(
            0,
            lastCommaIndex,
            true,
        )
    }

    throw IllegalStateException("Business metrics are incorrectly formatted: $metricsString")
}

/**
 * AWS SDK specific business metrics
 */
@InternalApi
public enum class AwsBusinessMetric(public override val identifier: String) : BusinessMetric {
    S3_EXPRESS_BUCKET("J"),
    DDB_MAPPER("d"),
    ;

    override fun toString(): String = identifier
}
