/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.restjson

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.http.*
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.IdentityDeserializer
import software.aws.clientrt.http.operation.UnitSerializer
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.operation.context
import software.aws.clientrt.http.operation.roundTrip
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsJsonTargetHeaderTest {

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
        op.install(AwsJsonTargetHeader) {
            version = "1.1"
        }

        val response = op.roundTrip(client, Unit)

        assertEquals("application/x-amz-json-1.1", response.request.headers["Content-Type"])
        assertEquals("FooService.Bar", response.request.headers["X-Amz-Target"])
    }
}
