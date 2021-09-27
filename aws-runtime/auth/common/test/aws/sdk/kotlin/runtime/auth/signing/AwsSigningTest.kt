/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AwsSigningTest {

    @Test
    fun testSignRequestSigV4() = runSuspendTest {
        // sanity test
        val request = HttpRequestBuilder().apply {
            method = HttpMethod.POST
            url.host = "http://demo.us-east-1.amazonaws.com"
            url.path = "/"
            headers.append("Host", "demo.us-east-1.amazonaws.com")
            headers.appendAll("x-amz-archive-description", listOf("test", "test"))
            val requestBody = "{\"TableName\": \"foo\"}"
            body = ByteArrayContent(requestBody.encodeToByteArray())
            headers.append("Content-Length", body.contentLength?.toString() ?: "0")
        }.build()

        val config = AwsSigningConfig {
            region = "us-east-1"
            service = "demo"
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
            date = Instant.fromIso8601("2020-10-16T19:56:00Z")
            credentialsProvider = TestCredentialsProvider
        }

        val result = sign(request, config)

        val expectedDate = "20201016T195600Z"
        val expectedSig = "e60a4adad4ae15e05c96a0d8ac2482fbcbd66c88647c4457db74e4dad1648608"
        val expectedAuth = "AWS4-HMAC-SHA256 Credential=AKID/20201016/us-east-1/demo/aws4_request, " +
            "SignedHeaders=content-length;host;x-amz-archive-description;x-amz-date;x-amz-security-token, " +
            "Signature=$expectedSig"

        assertEquals(expectedDate, result.output.headers["X-Amz-Date"])
        assertEquals(expectedAuth, result.output.headers["Authorization"])
        assertEquals(expectedSig, result.signature.decodeToString())
    }

    @Test
    fun testSignRequestSigV4Asymmetric() = runSuspendTest {
        // sanity test
        val request = HttpRequestBuilder().apply {
            method = HttpMethod.POST
            url.host = "http://demo.us-east-1.amazonaws.com"
            url.path = "/"
            headers.append("Host", "demo.us-east-1.amazonaws.com")
            headers.appendAll("x-amz-archive-description", listOf("test", "test"))
            val requestBody = "{\"TableName\": \"foo\"}"
            body = ByteArrayContent(requestBody.encodeToByteArray())
            headers.append("Content-Length", body.contentLength?.toString() ?: "0")
        }.build()

        val config = AwsSigningConfig {
            region = "us-east-1"
            service = "service"
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS
            algorithm = AwsSigningAlgorithm.SIGV4_ASYMMETRIC
            date = Instant.fromIso8601("2015-08-30T12:36:00Z")
            credentialsProvider = TestCredentialsProvider
        }

        val result = sign(request, config)

        val expectedPrefix = "AWS4-ECDSA-P256-SHA256 Credential=AKID/20150830/service/aws4_request, SignedHeaders=content-length;host;x-amz-archive-description;x-amz-date;x-amz-region-set;x-amz-security-token, Signature="
        val authHeader = result.output.headers["Authorization"]!!
        assertTrue(authHeader.contains(expectedPrefix), "Sigv4A auth header: $authHeader")
    }
}
