/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.http.Headers
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.HttpStatusCode
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.*
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.sdkHttpClient
import kotlin.test.Test
import kotlin.test.assertTrue

class UserAgentTest {

    @Test
    fun `it sets ua headers`() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(requestBuilder: HttpRequestBuilder): HttpResponse {
                return HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty, requestBuilder.build())
            }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "Test Service"
                operationName = "testOperation"
            }
        }

        op.install(UserAgent) {
            metadata = AwsUserAgentMetadata.fromEnvironment(ApiMetadata("Test Service", "1.2.3"))
        }

        val response = op.roundTrip(client, Unit)
        assertTrue(response.request.headers.contains(USER_AGENT))
        assertTrue(response.request.headers.contains(X_AMZ_USER_AGENT))
    }
}
