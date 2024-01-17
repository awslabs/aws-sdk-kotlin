/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithm
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.client.SdkClientOption
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class UnsupportedSigningAlgorithmInterceptorTest {
    @Test
    fun testUnsupportedSigningAlgorithmSigV4a() = runTest {
        val exception = assertFails {
            UnsupportedSigningAlgorithmInterceptor()
                .modifyBeforeCompletion(
                    context(
                        Result.failure(
                            UnsupportedSigningAlgorithm(
                                "SIGV4A support is not yet implemented for the default signer.",
                                true,
                            ),
                        ),
                    ),
                )
        }

        assertEquals(UnsupportedSigningAlgorithm::class, exception::class)
        assertEquals("SIGV4A support is not yet implemented for the default signer.", exception.message)
    }

    @Test
    fun testUnsupportedSigningAlgorithmNotSigV4aNoException() = runTest {
        UnsupportedSigningAlgorithmInterceptor()
            .modifyBeforeCompletion(
                context(
                    Result.failure(
                        UnsupportedSigningAlgorithm(
                            "SIGV5 support is not yet implemented for the default signer.",
                            false,
                        ),
                    ),
                ),
            )
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
