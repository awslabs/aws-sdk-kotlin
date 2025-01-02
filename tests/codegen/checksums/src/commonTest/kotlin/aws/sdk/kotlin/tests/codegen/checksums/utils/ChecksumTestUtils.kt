/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.tests.codegen.checksums.utils

import aws.sdk.kotlin.test.checksums.TestClient
import aws.sdk.kotlin.test.checksums.httpChecksumOperation
import aws.sdk.kotlin.test.checksums.httpChecksumRequestChecksumsNotRequiredOperation
import aws.sdk.kotlin.test.checksums.model.ChecksumAlgorithm
import aws.sdk.kotlin.test.checksums.model.ValidationMode
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetric
import aws.smithy.kotlin.runtime.businessmetrics.BusinessMetrics
import aws.smithy.kotlin.runtime.client.ProtocolRequestInterceptorContext
import aws.smithy.kotlin.runtime.client.config.RequestHttpChecksumConfig
import aws.smithy.kotlin.runtime.client.config.ResponseHttpChecksumConfig
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.httptest.TestEngine
import aws.smithy.kotlin.runtime.io.SdkSource
import aws.smithy.kotlin.runtime.io.source
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks if the specified headers are set in an HTTP request.
 */
internal class HeaderReader(
    private val expectedHeaders: Map<String, String?> = emptyMap(),
    private val forbiddenHeaders: Map<String, String?> = emptyMap(),
) : HttpInterceptor {
    var containsExpectedHeaders = false
    var containsForbiddenHeaders = true

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        expectedHeaders.forEach { header ->
            val containsHeader = context.protocolRequest.headers.contains(header.key)
            val headerValueMatches = header.value?.let { headerValue ->
                context.protocolRequest.headers[header.key] == headerValue
            } ?: true

            if (!containsHeader || !headerValueMatches) {
                return
            }
        }

        forbiddenHeaders.forEach { header ->
            if (context.protocolRequest.headers.contains(header.key)) {
                return
            }
        }

        containsExpectedHeaders = true
        containsForbiddenHeaders = false
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

/**
 * Checks if the specified business metrics are set in an HTTP request.
 */
internal class BusinessMetricsReader(
    private val expectedBusinessMetrics: Set<BusinessMetric>,
) : HttpInterceptor {
    var containsExpectedBusinessMetrics = false

    override fun readBeforeTransmit(context: ProtocolRequestInterceptorContext<Any, HttpRequest>) {
        containsExpectedBusinessMetrics = context.executionContext[BusinessMetrics].containsAll(expectedBusinessMetrics)
    }
}

/**
 * Runs a checksum test
 */
internal fun runChecksumTest(
    // Interceptors
    headerReader: HeaderReader? = null,
    headerSetter: HeaderSetter? = null,
    businessMetricsReader: BusinessMetricsReader? = null,
    // Config
    requestChecksumCalculationValue: RequestHttpChecksumConfig? = null,
    responseChecksumValidationValue: ResponseHttpChecksumConfig? = null,
    checksumAlgorithmValue: ChecksumAlgorithm? = null,
    validationModeValue: ValidationMode? = null,
    // Request/Response
    responseChecksumHeader: String? = null,
    responseChecksumValue: String? = null,
    requestBody: String = "Hello world",
    responseBody: String = "Hello world",
    // Test type
    requestChecksumRequired: Boolean = true,
): Unit = runBlocking {
    TestClient {
        httpClient = TestEngine()
        interceptors = listOfNotNull(headerReader, headerSetter, businessMetricsReader).toMutableList()
        requestChecksumCalculation = requestChecksumCalculationValue ?: requestChecksumCalculation
        responseChecksumValidation = responseChecksumValidationValue ?: responseChecksumValidation
        httpClient = TestEngine(
            roundTripImpl = { _, request ->
                val resp = HttpResponse(
                    HttpStatusCode.OK,
                    Headers {
                        append(responseChecksumHeader ?: "", responseChecksumValue ?: "")
                    },
                    object : HttpBody.SourceContent() {
                        override val isOneShot: Boolean = false
                        override val contentLength: Long? = responseBody.length.toLong()
                        override fun readFrom(): SdkSource = responseBody.toByteArray().source()
                    },
                )
                val now = Instant.now()
                HttpCall(request, resp, now, now)
            },
        )
    }.use { client ->
        if (requestChecksumRequired) {
            client.httpChecksumOperation {
                body = requestBody.encodeToByteArray()
                checksumAlgorithm = checksumAlgorithmValue ?: checksumAlgorithm
                validationMode = validationModeValue ?: validationMode
            }
        } else {
            client.httpChecksumRequestChecksumsNotRequiredOperation {
                body = requestBody.encodeToByteArray()
                checksumAlgorithm = checksumAlgorithmValue ?: checksumAlgorithm
                validationMode = validationModeValue ?: validationMode
            }
        }
    }

    businessMetricsReader?.let { assertTrue(it.containsExpectedBusinessMetrics) }
    headerReader?.let {
        assertTrue(it.containsExpectedHeaders)
        assertFalse(it.containsForbiddenHeaders)
    }
}
