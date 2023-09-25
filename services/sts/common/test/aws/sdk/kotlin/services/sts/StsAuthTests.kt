/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.sts

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.engine.callContext
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests related to STS model and whether requests need to be signed or not
 */
class StsAuthTests {

    private val mockEngine = object : HttpClientEngineBase("mock-engine") {
        var capturedRequest: HttpRequest? = null

        override val config: HttpClientEngineConfig = HttpClientEngineConfig.Default

        override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
            capturedRequest = request
            val callContext = callContext()
            val now = Instant.now()
            return HttpCall(request, HttpResponse(HttpStatusCode.OK, Headers.Empty, HttpBody.Empty), now, now, callContext)
        }
    }

    private val credentials = Credentials("ANOTREAL", "notrealrnrELgWzOk3IFjzDKtFBhDby", "notarealsessiontoken")

    @Test
    fun testAssumeRoleIsSigned(): Unit = runTest {
        val client = StsClient {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider(credentials)
            httpClient = mockEngine
        }

        runCatching { client.assumeRole { } }
        val request = assertNotNull(mockEngine.capturedRequest)
        assertTrue(request.headers.contains("AUTHORIZATION"))
    }

    @Test
    fun testWebIdentityIsUnsigned(): Unit = runTest {
        val client = StsClient {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider(credentials)
            httpClient = mockEngine
        }

        runCatching { client.assumeRoleWithWebIdentity { } }
        val request = assertNotNull(mockEngine.capturedRequest)
        assertFalse(request.headers.contains("AUTHORIZATION"), "assumeRoleWithWebIdentity should not require a signed request")
    }

    @Test
    fun testAssumeRoleSamlIsUnsigned(): Unit = runTest {
        val client = StsClient {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider(credentials)
            httpClient = mockEngine
        }

        runCatching { client.assumeRoleWithSaml { } }
        val request = assertNotNull(mockEngine.capturedRequest)
        assertFalse(request.headers.contains("AUTHORIZATION"), "assumeRoleWithSaml should not require a signed request")
    }
}
