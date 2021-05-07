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
            serviceShapeName = "FooService_blah"
            version = "1.1"
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request

        assertEquals("application/x-amz-json-1.1", request.headers["Content-Type"])
        // ensure we use the original shape id name, NOT the one from the context
        // see: https://github.com/awslabs/smithy-kotlin/issues/316
        assertEquals("FooService_blah.Bar", request.headers["X-Amz-Target"])
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
            serviceShapeName = "FooService"
            version = "1.1"
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
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
            serviceShapeName = "FooService"
            version = "1.1"
        }

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        val actual = request.body.readAll()?.decodeToString()
        assertEquals("application/xml", request.headers["Content-Type"])
        assertEquals("foo", actual)
    }
}
