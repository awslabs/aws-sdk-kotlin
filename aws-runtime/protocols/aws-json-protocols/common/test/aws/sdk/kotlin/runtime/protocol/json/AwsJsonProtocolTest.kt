/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.json

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
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.get
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsJsonProtocolTest {

    @Test
    fun testSetJsonProtocolHeaders() = runSuspendTest {
        val mockEngine = object : HttpClientEngineBase("test") {
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
        val m = AwsJsonProtocol("FooService_blah", "1.1")
        op.install(m)

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request

        assertEquals("application/x-amz-json-1.1", request.headers["Content-Type"])
        // ensure we use the original shape id name, NOT the one from the context
        // see: https://github.com/awslabs/smithy-kotlin/issues/316
        assertEquals("FooService_blah.Bar", request.headers["X-Amz-Target"])
    }

    @Test
    fun testEmptyBody() = runSuspendTest {
        val mockEngine = object : HttpClientEngineBase("test") {
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
        op.install(AwsJsonProtocol("FooService", "1.1"))

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        val actual = request.body.readAll()?.decodeToString()

        assertEquals("{}", actual)
    }

    @Test
    fun testDoesNotOverride() = runSuspendTest {
        val mockEngine = object : HttpClientEngineBase("test") {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val resp = HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
                val now = Instant.now()
                return HttpCall(request, resp, now, now)
            }
        }

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = object : HttpSerialize<Unit> {
                override suspend fun serialize(context: ExecutionContext, input: Unit): HttpRequestBuilder =
                    HttpRequestBuilder().apply {
                        headers["Content-Type"] = "application/xml"
                        body = ByteArrayContent("foo".encodeToByteArray())
                    }
            }
            deserializer = IdentityDeserializer
            context {
                service = "FooService"
                operationName = "Bar"
            }
        }
        val client = sdkHttpClient(mockEngine)
        op.install(AwsJsonProtocol("FooService", "1.1"))

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        val actual = request.body.readAll()?.decodeToString()
        assertEquals("application/xml", request.headers["Content-Type"])
        assertEquals("foo", actual)
    }
}
