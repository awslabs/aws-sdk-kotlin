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

/**
 * Appends business metrics to the `User-Agent` header.
 */
public class BusinessMetricsInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        context.executionContext.getOrNull(BusinessMetrics)?.let { metrics ->
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
    if (metrics.isEmpty()) return ""
    val metricsString = metrics.joinToString(",", "m/")
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
    ;

    public enum class Credentials(public override val identifier: String) : BusinessMetric {
        CREDENTIALS_CODE("e"),
        CREDENTIALS_JVM_SYSTEM_PROPERTIES("f"),
        CREDENTIALS_ENV_VARS("g"),
        CREDENTIALS_ENV_VARS_STS_WEB_ID_TOKEN("h"),
        CREDENTIALS_STS_ASSUME_ROLE("i"),
        CREDENTIALS_STS_ASSUME_ROLE_WEB_ID("k"),
        CREDENTIALS_PROFILE("n"),
        CREDENTIALS_PROFILE_SOURCE_PROFILE("o"),
        CREDENTIALS_PROFILE_NAMED_PROVIDER("p"),
        CREDENTIALS_PROFILE_STS_WEB_ID_TOKEN("q"),
        CREDENTIALS_PROFILE_SSO("r"),
        CREDENTIALS_SSO("s"),
        CREDENTIALS_PROFILE_SSO_LEGACY("t"),
        CREDENTIALS_SSO_LEGACY("u"),
        CREDENTIALS_PROFILE_PROCESS("v"),
        CREDENTIALS_PROCESS("w"),
        CREDENTIALS_HTTP("z"),
        CREDENTIALS_IMDS("0"),
    }
}
