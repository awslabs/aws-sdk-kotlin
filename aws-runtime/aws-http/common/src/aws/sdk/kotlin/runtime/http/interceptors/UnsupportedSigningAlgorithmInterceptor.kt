/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.http.interceptors

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.auth.awssigning.UnsupportedSigningAlgorithm
import aws.smithy.kotlin.runtime.client.ResponseInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse

// TODO: Remove me once sigV4a is supported by default AWS signer
/**
 * Looks for an unsupported signing algorithm error caused by sigV4a.
 * If so it sends users to a page in the AWS SDK for Kotlin documentation on how to fix it.
 */
@InternalSdkApi
public class UnsupportedSigningAlgorithmInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeCompletion(context: ResponseInterceptorContext<Any, Any, HttpRequest?, HttpResponse?>): Result<Any> {
        context.response.exceptionOrNull()?.let {
            if (it is UnsupportedSigningAlgorithm && it.isSigV4a) {
                throw UnsupportedSigningAlgorithm(
                    it.message + " Please follow the AWS SDK for Kotlin developer guide to set it up with the CRT signer. **LINK TO GUIDE**",
                    true,
                )
            }
        }
        return super.modifyBeforeCompletion(context)
    }
}
