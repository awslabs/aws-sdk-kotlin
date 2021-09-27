/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.io.SdkByteReadChannel
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.get
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsSigv4SigningMiddlewareTest {

    private fun buildOperation(streaming: Boolean = false, replayable: Boolean = true): SdkHttpOperation<Unit, HttpResponse> = SdkHttpOperation.build {
        serializer = object : HttpSerialize<Unit> {
            override suspend fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder =
                HttpRequestBuilder().apply {
                    method = HttpMethod.POST
                    url.host = "http://demo.us-east-1.amazonaws.com"
                    url.path = "/"
                    headers.append("Host", "demo.us-east-1.amazonaws.com")
                    headers.appendAll("x-amz-archive-description", listOf("test", "test"))
                    val requestBody = "{\"TableName\": \"foo\"}"
                    body = when (streaming) {
                        true -> object : HttpBody.Streaming() {
                            override val contentLength: Long = requestBody.length.toLong()
                            override fun readFrom(): SdkByteReadChannel = SdkByteReadChannel(requestBody.encodeToByteArray())
                            override val isReplayable: Boolean = replayable
                            override fun reset() { }
                        }
                        false -> ByteArrayContent(requestBody.encodeToByteArray())
                    }
                    headers.append("Content-Length", body.contentLength?.toString() ?: "0")
                }
        }
        deserializer = IdentityDeserializer

        context {
            operationName = "testSigningOperation"
            service = "TestService"
            set(AuthAttributes.SigningRegion, "us-east-1")
            set(AuthAttributes.SigningDate, Instant.fromIso8601("2020-10-16T19:56:00Z"))
            set(AuthAttributes.SigningService, "demo")
        }
    }

    private suspend fun getSignedRequest(operation: SdkHttpOperation<Unit, HttpResponse>): HttpRequest {
        val mockEngine = object : HttpClientEngineBase("test") {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val now = Instant.now()
                val resp = HttpResponse(HttpStatusCode.fromValue(200), Headers.Empty, HttpBody.Empty)
                return HttpCall(request, resp, now, now)
            }
        }
        val client = sdkHttpClient(mockEngine)

        operation.install(AwsSigV4SigningMiddleware) {
            credentialsProvider = TestCredentialsProvider
            signingService = "demo"
        }

        operation.roundTrip(client, Unit)
        return operation.context[HttpOperationContext.HttpCallList].last().request
    }

    @Test
    fun testSignRequest() = runSuspendTest {
        val op = buildOperation()
        val expectedDate = "20201016T195600Z"
        val expectedSig = "AWS4-HMAC-SHA256 Credential=AKID/20201016/us-east-1/demo/aws4_request, " +
            "SignedHeaders=content-length;host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
            "Signature=e60a4adad4ae15e05c96a0d8ac2482fbcbd66c88647c4457db74e4dad1648608"

        val signed = getSignedRequest(op)
        assertEquals(expectedDate, signed.headers["X-Amz-Date"])
        assertEquals(expectedSig, signed.headers["Authorization"])
    }

    @Test
    fun testUnsignedRequest() = runSuspendTest {
        val op = buildOperation()
        val expectedDate = "20201016T195600Z"
        val expectedSig = "AWS4-HMAC-SHA256 Credential=AKID/20201016/us-east-1/demo/aws4_request, " +
            "SignedHeaders=content-length;host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
            "Signature=6c0cc11630692e2c98f28003c8a0349b56011361e0bab6545f1acee01d1d211e"

        op.context[AuthAttributes.UnsignedPayload] = true

        val signed = getSignedRequest(op)
        assertEquals(expectedDate, signed.headers["X-Amz-Date"])
        assertEquals(expectedSig, signed.headers["Authorization"])
    }

    @Test
    fun testSignReplayableStreamingRequest() = runSuspendTest {
        val op = buildOperation(streaming = true)
        val expectedDate = "20201016T195600Z"
        val expectedSig = "AWS4-HMAC-SHA256 Credential=AKID/20201016/us-east-1/demo/aws4_request, " +
            "SignedHeaders=content-length;host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
            "Signature=e60a4adad4ae15e05c96a0d8ac2482fbcbd66c88647c4457db74e4dad1648608"

        val signed = getSignedRequest(op)
        assertEquals(expectedDate, signed.headers["X-Amz-Date"])
        assertEquals(expectedSig, signed.headers["Authorization"])
    }

    @Test
    fun testSignOneShotStream() = runSuspendTest {
        val op = buildOperation(streaming = true, replayable = false)
        val expectedDate = "20201016T195600Z"
        // should have same signature as testUnsignedRequest()
        val expectedSig = "AWS4-HMAC-SHA256 Credential=AKID/20201016/us-east-1/demo/aws4_request, " +
            "SignedHeaders=content-length;host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
            "Signature=6c0cc11630692e2c98f28003c8a0349b56011361e0bab6545f1acee01d1d211e"

        val signed = getSignedRequest(op)
        assertEquals(expectedDate, signed.headers["X-Amz-Date"])
        assertEquals(expectedSig, signed.headers["Authorization"])
    }
}
