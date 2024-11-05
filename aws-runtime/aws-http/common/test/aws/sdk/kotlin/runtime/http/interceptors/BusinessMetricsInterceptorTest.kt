/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.http.BUSINESS_METRICS_MAX_LENGTH
import aws.sdk.kotlin.runtime.http.middleware.USER_AGENT
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.SmithyBusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessMetricsInterceptorTest {
    @Test
    fun noBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()
        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertFalse(userAgentHeader.endsWith("m/"))
    }

    @Test
    fun businessMetrics() = runTest {
        val executionContext = ExecutionContext()
        executionContext.emitBusinessMetric(AwsBusinessMetric.S3_EXPRESS_BUCKET)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertTrue(
            userAgentHeader.endsWith(
                "m/${AwsBusinessMetric.S3_EXPRESS_BUCKET.identifier}",
            ),
        )
    }

    @Test
    fun multipleBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()
        executionContext.emitBusinessMetric(AwsBusinessMetric.S3_EXPRESS_BUCKET)
        executionContext.emitBusinessMetric(SmithyBusinessMetric.GZIP_REQUEST_COMPRESSION)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!

        assertTrue(
            userAgentHeader.endsWith(
                "m/${AwsBusinessMetric.S3_EXPRESS_BUCKET.identifier},${SmithyBusinessMetric.GZIP_REQUEST_COMPRESSION.identifier}",
            ),
        )
    }

    @Test
    fun truncateBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()
        executionContext.attributes[aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics] = mutableSetOf()

        for (i in 0..1024) {
            executionContext.emitBusinessMetric(
                object : BusinessMetric {
                    override val identifier: String = i.toString()
                },
            )
        }

        val rawMetrics = executionContext[aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics]
        val rawMetricsString = rawMetrics.joinToString(",", "m/")
        val rawMetricsByteArray = rawMetricsString.encodeToByteArray()

        assertTrue(rawMetricsByteArray.size >= BUSINESS_METRICS_MAX_LENGTH)

        val interceptor = BusinessMetricsInterceptor()
        val request = interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        val userAgentHeader = request.headers[USER_AGENT]!!
        val truncatedMetrics = "m/" + userAgentHeader.substringAfter("m/")

        assertTrue(truncatedMetrics.encodeToByteArray().size <= BUSINESS_METRICS_MAX_LENGTH)
        assertFalse(truncatedMetrics.endsWith(","))
    }

    @Test
    fun malformedBusinessMetrics() = runTest {
        val executionContext = ExecutionContext()
        val reallyLongMetric = "All work and no play makes Jack a dull boy".repeat(1000)

        executionContext.attributes.emitBusinessMetric(
            object : BusinessMetric {
                override val identifier: String = reallyLongMetric
            },
        )

        val interceptor = BusinessMetricsInterceptor()

        assertFailsWith<IllegalStateException>("Business metrics are incorrectly formatted:") {
            interceptor.modifyBeforeTransmit(interceptorContext(executionContext))
        }
    }
}

private fun interceptorContext(executionContext: ExecutionContext): ProtocolRequestInterceptorContext<Any, HttpRequest> =
    object : ProtocolRequestInterceptorContext<Any, HttpRequest> {
        override val protocolRequest: HttpRequest = HttpRequest(
            HttpMethod.GET,
            Url.parse("https://test.aws.com?foo=bar"),
            Headers {
                append(USER_AGENT, "aws-sdk-kotlin/1.2.3 ua/2.1 api/test-service#1.2.3...")
            },
        )
        override val executionContext: ExecutionContext = executionContext
        override val request: Any = Unit
    }
