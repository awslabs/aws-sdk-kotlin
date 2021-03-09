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
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsJsonProtocolTest {

    @Test
    fun testSetJsonProtocolHeaders() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                return HttpResponse(HttpStatusCode.OK, Headers {}, HttpBody.Empty, requestBuilder.build())
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

        val response = op.roundTrip(client, Unit)

        assertEquals("application/x-amz-json-1.1", response.request.headers["Content-Type"])
        assertEquals("FooService.Bar", response.request.headers["X-Amz-Target"])
    }

    @Test
    fun testEmptyBody() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                return HttpResponse(HttpStatusCode.OK, Headers {}, HttpBody.Empty, requestBuilder.build())
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

        val response = op.roundTrip(client, Unit)
        val actual = response.request.body.readAll()?.decodeToString()

        assertEquals("{}", actual)
    }

    @Test
    fun testDoesNotOverride() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                return HttpResponse(HttpStatusCode.OK, Headers {}, HttpBody.Empty, requestBuilder.build())
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

        val response = op.roundTrip(client, Unit)
        val actual = response.request.body.readAll()?.decodeToString()
        assertEquals("application/xml", response.request.headers["Content-Type"])
        assertEquals("foo", actual)
    }
}
