/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.restjson

import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.client.SdkClientOption
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.request.HttpRequestContext
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.sdkHttpClient
import software.aws.kotlinsdk.testing.runSuspendTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AwsJsonTargetHeaderTest {

    @Test
    fun testSetJsonProtocolHeaders() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse { throw NotImplementedError() }
        }
        val client = sdkHttpClient(mockEngine) {
            install(AwsJsonTargetHeader) {
                version = "1.1"
            }
        }

        val executionContext = ExecutionContext.build {
            attributes[SdkClientOption.ServiceName] = "FooService"
            attributes[SdkClientOption.OperationName] = "Bar"
        }

        val builder = client.requestPipeline.execute(HttpRequestContext(executionContext), HttpRequestBuilder())

        assertEquals("application/x-amz-json-1.1", builder.headers["Content-Type"])
        assertEquals("FooService.Bar", builder.headers["X-Amz-Target"])
    }
}
