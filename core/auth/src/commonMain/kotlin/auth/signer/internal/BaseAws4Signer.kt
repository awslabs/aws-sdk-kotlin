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
package auth.signer.internal

import annotations.SdkInternalApi
import auth.credentials.CredentialUtils
import auth.signer.params.Aws4SignerParams
import core.interceptor.ExecutionAttributes
import http.SdkHttpFullRequest

/**
 * Abstract base class for concrete implementations of Aws4 signers.
 */
abstract class BaseAws4Signer : AbstractAws4Signer<Aws4SignerParams?, Aws4PresignerParams?>() {
    fun sign(request: SdkHttpFullRequest?, executionAttributes: ExecutionAttributes?): SdkHttpFullRequest {
        val signingParams: Aws4SignerParams = extractSignerParams<B>(Aws4SignerParams.builder(), executionAttributes)
            .build()
        return sign(request, signingParams)
    }

    fun sign(request: SdkHttpFullRequest, signingParams: Aws4SignerParams): SdkHttpFullRequest {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request
        }
        val requestParams: auth.signer.internal.Aws4SignerRequestParams =
            auth.signer.internal.Aws4SignerRequestParams(signingParams)
        return doSign(request, requestParams, signingParams).build()
    }

    fun presign(requestToSign: SdkHttpFullRequest?, executionAttributes: ExecutionAttributes?): SdkHttpFullRequest {
        val signingParams: Aws4PresignerParams = extractPresignerParams<B>(
            Aws4PresignerParams.builder(),
            executionAttributes
        )
            .build()
        return presign(requestToSign, signingParams)
    }

    fun presign(request: SdkHttpFullRequest, signingParams: Aws4PresignerParams): SdkHttpFullRequest {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request
        }
        val requestParams: auth.signer.internal.Aws4SignerRequestParams =
            auth.signer.internal.Aws4SignerRequestParams(signingParams)
        return doPresign(request, requestParams, signingParams).build()
    }

    /**
     * Subclass could override this method to perform any additional procedure
     * on the request payload, with access to the result from signing the
     * header. (e.g. Signing the payload by chunk-encoding). The default
     * implementation doesn't need to do anything.
     */
    protected override fun processRequestPayload(
        mutableRequest: SdkHttpFullRequest.Builder,
        signature: ByteArray,
        signingKey: ByteArray,
        signerRequestParams: auth.signer.internal.Aws4SignerRequestParams,
        signerParams: Aws4SignerParams
    ) {
    }

    /**
     * Calculate the hash of the request's payload. In case of pre-sign, the
     * existing code would generate the hash of an empty byte array and returns
     * it. This method can be overridden by sub classes to provide different
     * values (e.g) For S3 pre-signing, the content hash calculation is
     * different from the general implementation.
     */
    protected override fun calculateContentHashPresign(
        mutableRequest: SdkHttpFullRequest.Builder,
        signerParams: Aws4PresignerParams
    ): String {
        return calculateContentHash(mutableRequest, signerParams)
    }
}