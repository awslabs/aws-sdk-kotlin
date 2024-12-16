/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors.businessmetrics

import aws.sdk.kotlin.runtime.http.middleware.USER_AGENT
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
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
