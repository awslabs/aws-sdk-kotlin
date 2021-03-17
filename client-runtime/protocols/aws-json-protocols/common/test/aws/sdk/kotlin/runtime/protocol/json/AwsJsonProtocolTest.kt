/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.json

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.*
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.*
import software.aws.clientrt.http.request.HttpRequest
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpCall
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.time.Instant
import software.aws.clientrt.util.get
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsJsonProtocolTest {

    @Test
    fun testSetJsonProtocolHeaders() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val resp = HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
                val now = Instant.now()
                return HttpCall(request, resp, now, now)
            }
        }

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "FooService"
                operationName = "Bar"
            }
        }
        val client = sdkHttpClient(mockEngine)
        op.install(AwsJsonProtocol) {
            version = "1.1"
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCalls].last().request

        assertEquals("application/x-amz-json-1.1", request.headers["Content-Type"])
        assertEquals("FooService.Bar", request.headers["X-Amz-Target"])
    }

    @Test
    fun testEmptyBody() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val resp = HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
                val now = Instant.now()
                return HttpCall(request, resp, now, now)
            }
        }

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "FooService"
                operationName = "Bar"
            }
        }
        val client = sdkHttpClient(mockEngine)
        op.install(AwsJsonProtocol) {
            version = "1.1"
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCalls].last().request
        val actual = request.body.readAll()?.decodeToString()

        assertEquals("{}", actual)
    }

    @Test
    fun testDoesNotOverride() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val resp = HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
                val now = Instant.now()
                return HttpCall(request, resp, now, now)
            }
        }

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = object : HttpSerialize<Unit> {
                override suspend fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder {
                    return HttpRequestBuilder().apply {
                        headers["Content-Type"] = "application/xml"
                        body = ByteArrayContent("foo".encodeToByteArray())
                    }
                }
            }
            deserializer = IdentityDeserializer
            context {
                service = "FooService"
                operationName = "Bar"
            }
        }
        val client = sdkHttpClient(mockEngine)
        op.install(AwsJsonProtocol) {
            version = "1.1"
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCalls].last().request
        val actual = request.body.readAll()?.decodeToString()
        assertEquals("application/xml", request.headers["Content-Type"])
        assertEquals("foo", actual)
    }
}
