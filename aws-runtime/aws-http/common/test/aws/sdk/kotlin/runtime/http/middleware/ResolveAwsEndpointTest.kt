/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.sdk.kotlin.runtime.endpoint.CredentialScope
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpCall
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.get
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class ResolveAwsEndpointTest {

    private val mockEngine = object : HttpClientEngineBase("test") {
        override suspend fun roundTrip(request: HttpRequest): HttpCall {
            val resp = HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty)
            return HttpCall(request, resp, Instant.now(), Instant.now())
        }
    }
    private val client = sdkHttpClient(mockEngine)

    @Test
    fun testHostIsSet(): Unit = aws.smithy.kotlin.runtime.testing.runSuspendTest {
        // sanity check - most of this is covered by setRequestEndpoint() from smithy-kotlin runtime
        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"

                set(AwsClientOption.Region, "us-east-1")
            }
        }

        val endpoint = AwsEndpoint("https://api.test.com")
        op.install(ResolveAwsEndpoint) {
            resolver = AwsEndpointResolver { _, _ -> endpoint }
            serviceId = "TestService"
        }

        op.roundTrip(client, Unit)
        val actual = op.context[HttpOperationContext.HttpCallList].first().request

        assertEquals("api.test.com", actual.url.host)
        assertEquals(Protocol.HTTPS, actual.url.scheme)
        assertEquals("api.test.com", actual.headers["Host"])
    }

    @Test
    fun testOverrideCredentialScopes(): Unit = runSuspendTest {
        // if an endpoint specifies credential scopes we should override the context
        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"

                set(AwsClientOption.Region, "us-east-1")
                set(AuthAttributes.SigningRegion, "us-east-1")
                set(AuthAttributes.SigningService, "quux")
            }
        }

        val endpoint = AwsEndpoint("https://api.test.com", CredentialScope("us-west-2", "foo"))
        op.install(ResolveAwsEndpoint) {
            resolver = AwsEndpointResolver { _, _ -> endpoint }
            serviceId = "TestService"
        }

        op.roundTrip(client, Unit)
        val actual = op.context[HttpOperationContext.HttpCallList].first().request

        assertEquals("api.test.com", actual.url.host)
        assertEquals(Protocol.HTTPS, actual.url.scheme)
        assertEquals("api.test.com", actual.headers["Host"])

        op.roundTrip(client, Unit)
        assertEquals("foo", op.context[AuthAttributes.SigningService])
        assertEquals("us-west-2", op.context[AuthAttributes.SigningRegion])
    }
}
