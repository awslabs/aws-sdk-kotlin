/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.middleware

import aws.sdk.kotlin.runtime.client.AwsClientOption
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
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
import kotlin.test.assertNotNull

@OptIn(ExperimentalStdlibApi::class)
class ServiceEndpointResolverTest {

    @Test
    fun `it sets the host to the expected endpoint`(): Unit = runSuspendTest {
        val expectedHost = "test.com"
        val mockEngine = object : HttpClientEngineBase("test") {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                assertEquals(expectedHost, request.url.host)
                assertEquals(expectedHost, request.headers["Host"])
                assertEquals("https", request.url.scheme.protocolName)
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
                service = "TestService"
                operationName = "testOperation"
            }
        }

        op.install(ServiceEndpointResolver) {
            serviceId = "TestService"
            resolver = object : EndpointResolver {
                override suspend fun resolve(service: String, region: String): Endpoint =
                    Endpoint("test.com", "https")
            }
        }

        op.context[AwsClientOption.Region] = "us-east-1"
        val response = op.roundTrip(client, Unit)
        assertNotNull(response)
    }

    @Test
    fun `it prepends hostPrefix when present`(): Unit = runSuspendTest {
        val expectedHost = "prefix.test.com"
        val mockEngine = object : HttpClientEngineBase("test") {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                assertEquals(expectedHost, request.url.host)
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
                service = "TestService"
                operationName = "testOperation"
                set(AwsClientOption.Region, "us-east-1")
                set(HttpOperationContext.HostPrefix, "prefix.")
            }
        }
        op.install(ServiceEndpointResolver) {
            serviceId = "TestService"
            resolver = object : EndpointResolver {
                override suspend fun resolve(service: String, region: String): Endpoint =
                    Endpoint("test.com", "https")
            }
        }

        val response = op.roundTrip(client, Unit)
        assertNotNull(response)
    }

    @Test
    fun `it overrides credential scopes`(): Unit = runSuspendTest {
        // if an endpoint specifies credential scopes we should override the context
        val expectedHost = "test.com"
        val mockEngine = object : HttpClientEngineBase("test") {
            override suspend fun roundTrip(request: HttpRequest): HttpCall {
                assertEquals(expectedHost, request.url.host)
                assertEquals(expectedHost, request.headers["Host"])
                assertEquals("https", request.url.scheme.protocolName)
                val resp = HttpResponse(HttpStatusCode.fromValue(200), Headers {}, HttpBody.Empty)
                val now = Instant.now()
                return HttpCall(request, resp, now, now)
            }
        }

        val client = sdkHttpClient(mockEngine)

        val op = SdkHttpOperation.build<Unit, HttpResponse> {
            serializer = UnitSerializer
            deserializer = IdentityDeserializer
            context {
                service = "TestService"
                operationName = "testOperation"
            }
        }

        op.install(ServiceEndpointResolver) {
            serviceId = "TestService"
            resolver = object : EndpointResolver {
                override suspend fun resolve(service: String, region: String): Endpoint =
                    Endpoint("test.com", "https", signingName = "foo", signingRegion = "us-west-2")
            }
        }

        op.context[AwsClientOption.Region] = "us-east-1"
        op.context[AuthAttributes.SigningRegion] = "us-east-1"
        op.context[AuthAttributes.SigningService] = "quux"
        op.roundTrip(client, Unit)
        assertEquals("foo", op.context[AuthAttributes.SigningService])
        assertEquals("us-west-2", op.context[AuthAttributes.SigningRegion])
    }
}
