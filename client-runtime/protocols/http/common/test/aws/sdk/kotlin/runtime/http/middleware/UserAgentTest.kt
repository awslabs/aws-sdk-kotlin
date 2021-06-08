/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.http.ApiMetadata
import aws.sdk.kotlin.runtime.http.AwsUserAgentMetadata
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import software.aws.clientrt.http.Headers
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.HttpStatusCode
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.operation.*
import software.aws.clientrt.http.request.HttpRequest
import software.aws.clientrt.http.response.HttpCall
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.http.sdkHttpClient
import software.aws.clientrt.time.Instant
import software.aws.clientrt.util.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserAgentTest {

    @Test
    fun `it sets ua headers`() = runSuspendTest {
        val mockEngine = object : HttpClientEngine {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                val resp = HttpResponse(HttpStatusCode.fromValue(200), Headers.Empty, HttpBody.Empty)
                val now = Instant.now()
                return HttpCall(request, resp, now, now)
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

        op.roundTrip(client, Unit)
        val request = op.context[HttpOperationContext.HttpCallList].last().request
        assertTrue(request.headers.contains(USER_AGENT))
        assertTrue(request.headers.contains(X_AMZ_USER_AGENT))
        assertEquals("aws-sdk-kotlin/1.2.3", request.headers[X_AMZ_USER_AGENT])
        assertTrue(request.headers[USER_AGENT]!!.startsWith("aws-sdk-kotlin/1.2.3 api/test-service/1.2.3"))
    }
}
