/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.http.middleware.USER_AGENT
import aws.smithy.kotlin.runtime.businessmetrics.businessMetrics
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder

/**
 * Appends business metrics to the `User-Agent` header.
 */
public class BusinessMetricsInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        context.executionContext.getOrNull(businessMetrics)?.let { metrics ->
            val metricsString = formatMetrics(metrics)
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
 */
private fun formatMetrics(metrics: MutableSet<String>): String {
    val metricsString = metrics.joinToString(",", "m/")
    val metricsByteArray = metricsString.toByteArray()
    val maxSize = 1024

    if (metricsByteArray.size <= maxSize) return metricsString

    val commaByte = ','.code.toByte()
    var lastCommaIndex: Int? = null

    for (i in 0..1023) {
        if (metricsByteArray[i] == commaByte) {
            lastCommaIndex = i
        }
    }

    lastCommaIndex?.let {
        return metricsByteArray.decodeToString(
            0,
            lastCommaIndex,
            true,
        )
    }

    throw IllegalStateException("Business metrics are incorrectly formatted: $metricsString")
}
