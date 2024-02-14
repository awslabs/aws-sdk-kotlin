/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAlgorithm
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithmException
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UnsupportedSigningAlgorithmInterceptorTest {
    @Test
    fun testUnsupportedSigningAlgorithmSigV4a() = runTest {
        val result =
            UnsupportedSigningAlgorithmInterceptor()
                .modifyBeforeCompletion(
                    context(
                        Result.failure(
                            UnsupportedSigningAlgorithmException(
                                "SIGV4A support is not yet implemented for the default signer.",
                                AwsSigningAlgorithm.SIGV4_ASYMMETRIC,
                            ),
                        ),
                    ),
                )

        val exception = result.exceptionOrNull()

        assertTrue(result.isFailure)
        assertIs<UnsupportedSigningAlgorithmException>(exception)
        assertEquals(exception.signingAlgorithm, AwsSigningAlgorithm.SIGV4_ASYMMETRIC)
        assertEquals(
            "SIGV4A support is not yet implemented for the default signer. For more information on how to enable it with the CRT signer, please refer to: https://a.co/3sf8533",
            exception.message,
        )
    }

    @Test
    fun testUnsupportedSigningAlgorithmNotSigV4a() = runTest {
        val result =
            UnsupportedSigningAlgorithmInterceptor()
                .modifyBeforeCompletion(
                    context(
                        Result.failure(
                            UnsupportedSigningAlgorithmException(
                                "SIGV4 support is not yet implemented for the default signer.",
                                AwsSigningAlgorithm.SIGV4,
                            ),
                        ),
                    ),
                )

        val exception = result.exceptionOrNull()

        assertTrue(result.isFailure)
        assertIs<UnsupportedSigningAlgorithmException>(exception)
        assertEquals(exception.signingAlgorithm, AwsSigningAlgorithm.SIGV4)
        assertEquals("SIGV4 support is not yet implemented for the default signer.", exception.message)
    }
}

private fun context(response: Result<Any>) =
    object : ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?> {
        override val executionContext = ExecutionContext.build { attributes[SdkClientOption.OperationName] = "test" }
        override val request = Unit
        override val response = response
        override val protocolRequest = HttpRequest { }
        override val protocolResponse = null
    }
