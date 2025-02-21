/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningAlgorithm
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithmException
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse

// FIXME: Remove this once sigV4a is supported by default AWS signer
/**
 * Looks for an unsupported signing algorithm error caused by sigV4a.
 * If so it sends users to a section in the AWS SDK for Kotlin documentation on how to fix it.
 */
@InternalSdkApi
@Deprecated("This interceptor is no longer used. It will be removed in the next minor version, v1.5.x.")
public class UnsupportedSigningAlgorithmInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeCompletion(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>): Result<Any> {
        context.response.exceptionOrNull()?.let {
            if (it is UnsupportedSigningAlgorithmException && it.signingAlgorithm == AwsSigningAlgorithm.SIGV4_ASYMMETRIC) {
                return Result.failure(
                    UnsupportedSigningAlgorithmException(
                        "SIGV4A support is not yet implemented for the default signer. For more information on how to enable it with the CRT signer, please refer to: https://a.co/3sf8533",
                        it.signingAlgorithm,
                        it,
                    ),
                )
            }
        }
        return super.modifyBeforeCompletion(context)
    }
}
