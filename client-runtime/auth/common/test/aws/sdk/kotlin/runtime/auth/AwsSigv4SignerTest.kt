/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.*
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.HttpSerialize
import software.aws.clientrt.http.operation.IdentityDeserializer
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.operation.context
import software.aws.clientrt.http.operation.roundTrip
import software.aws.clientrt.http.request.HttpRequest
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsSigv4SignerTest {

    private object TestCredentialsProvider : CredentialsProvider {
        val testCredentials = Credentials("AKID", "SECRET", "SESSION")
        override suspend fun getCredentials(): Credentials = testCredentials
    }

    private fun buildOperation(): SdkHttpOperation<Unit, HttpResponse> {
        return SdkHttpOperation.build {
            serializer = object : HttpSerialize<Unit> {
                override suspend fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder {
                    return HttpRequestBuilder().apply {
                        method = HttpMethod.POST
                        url.host = "http://demo.us-east-1.amazonaws.com"
                        url.path = "/"
                        headers.append("Host", "demo.us-east-1.amazonaws.com")
                        headers.appendAll("x-amz-archive-description", listOf("test", "test"))
                        val requestBody = "{\"TableName\": \"foo\"}"
                        body = ByteArrayContent(requestBody.encodeToByteArray())
                        headers.append("Content-Length", body.contentLength?.toString() ?: "0")
                    }
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
    }

    private suspend fun getSignedRequest(operation: SdkHttpOperation<Unit, HttpResponse>): HttpRequest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                return HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())
            }
        }
        val client = sdkHttpClient(mockEngine)

        operation.install(AwsSigv4Signer) {
            credentialsProvider = TestCredentialsProvider
            signingService = "demo"
        }

        val response = operation.roundTrip(client, Unit)
        return response.request
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
}
