/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package auth.signer

import annotations.SdkPublicApi
import auth.signer.internal.BaseAws4Signer
import auth.signer.internal.SignerConstant.X_AMZ_CONTENT_SHA256
import auth.signer.params.Aws4SignerParams
import core.interceptor.ExecutionAttributes
import http.SdkHttpFullRequest
import java.io.IOException
import kotlin.jvm.Throws

/**
 * Exactly the same as [Aws4Signer] except if the request is being sent
 * over HTTPS, then it returns the string `UNSIGNED-PAYLOAD` as the
 * content SHA-256 so services that support it can avoid needing to calculate
 * the value when authorizing the request.
 *
 *
 * Payloads are still signed for requests over HTTP to preserve the request
 * integrity over a non-secure transport.
 */
@SdkPublicApi
class Aws4UnsignedPayloadSigner private constructor() : BaseAws4Signer() {
    fun sign(request: SdkHttpFullRequest, executionAttributes: ExecutionAttributes?): SdkHttpFullRequest {
        var request: SdkHttpFullRequest = request
        request = addContentSha256Header(request)
        return super.sign(request, executionAttributes)
    }

    fun sign(request: SdkHttpFullRequest, signingParams: Aws4SignerParams?): SdkHttpFullRequest {
        var request: SdkHttpFullRequest = request
        request = addContentSha256Header(request)
        return super.sign(request, signingParams)
    }

    protected fun calculateContentHash(
        mutableRequest: SdkHttpFullRequest.Builder,
        signerParams: Aws4SignerParams?
    ): String {
        return if ("https" == mutableRequest.protocol()) {
            "UNSIGNED-PAYLOAD"
        } else super.calculateContentHash(mutableRequest, signerParams)
    }

    private fun addContentSha256Header(request: SdkHttpFullRequest): SdkHttpFullRequest {
        return request.toBuilder().putHeader(X_AMZ_CONTENT_SHA256, "required").build()
    }

    companion object {
        fun create(): Aws4UnsignedPayloadSigner {
            return Aws4UnsignedPayloadSigner()
        }
    }
}