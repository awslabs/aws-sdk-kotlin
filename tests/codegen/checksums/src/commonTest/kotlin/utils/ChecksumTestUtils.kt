package utils

import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import kotlinx.coroutines.runBlocking

/**
 * Checks if the specified headers are set in an HTTP request.
 */
internal class HeaderReader(
    private val expectedHeaders: Map<String, String?>,
    private val forbiddenHeaders: Map<String, String?> = emptyMap(),
) : HttpInterceptor {
    var containsExpectedHeaders = true
    var containsForbiddenHeaders = false

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        expectedHeaders.forEach { header ->
            val containsHeader = context.protocolRequest.headers.contains(header.key)
            val headerValueMatches = header.value?.let { headerValue ->
                context.protocolRequest.headers[header.key] == headerValue
            } ?: true

            if (!containsHeader || !headerValueMatches) {
                containsExpectedHeaders = false
                return
            }
        }

        forbiddenHeaders.forEach { header ->
            if (context.protocolRequest.headers.contains(header.key)) {
                containsForbiddenHeaders = true
                return
            }
        }
    }
}

/**
 * Checks if the specified trailer headers are set in an HTTP request.
 */
internal class TrailerReader(
    private val expectedTrailers: Map<String, String?>,
) : HttpInterceptor {
    var containsExpectedTrailers = true

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        expectedTrailers.forEach { trailer ->
            val containsTrailer = context.protocolRequest.trailingHeaders.contains(trailer.key)
            val trailerValueMatches = trailer.value?.let { trailerValue ->
                runBlocking {
                    context.protocolRequest.trailingHeaders[trailer.key]?.await() == trailerValue
                }
            } ?: true

            if (!containsTrailer || !trailerValueMatches) {
                containsExpectedTrailers = false
                return
            }
        }
    }
}

/**
 * Sets the specified checksum header and value in an HTTP request.
 */
internal class HeaderSetter(
    private val headers: Map<String, String>,
) : HttpInterceptor {
    override suspend fun modifyBeforeRetryLoop(context: ProtocolRequestInterceptorContext<Any, HttpRequest>): HttpRequest {
        val request = context.protocolRequest.toBuilder()
        headers.forEach {
            request.headers[it.key] = it.value
        }
        return request.build()
    }
}

internal class BusinessMetricsReader(
    private val expectedBusinessMetrics: Set<BusinessMetric>,
) : HttpInterceptor {
    var containsExpectedBusinessMetrics = true

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        containsExpectedBusinessMetrics = context.executionContext[BusinessMetrics].containsAll(expectedBusinessMetrics)
    }
}
