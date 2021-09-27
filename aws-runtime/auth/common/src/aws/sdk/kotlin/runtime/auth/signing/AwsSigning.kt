/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.runtime.crt.toSignableCrtRequest
import aws.sdk.kotlin.runtime.crt.update
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.request.toBuilder

/**
 * Container for signed output and signature
 *
 * @property output The signed output type (e.g. HttpRequest)
 * @property signature The resulting signature. Depending on the requested signature type and algorithm,
 * this value will be in one of the following formats:
 *
 *   1. [AwsSignatureType.HTTP_REQUEST_VIA_HEADERS] - hex encoding of the binary signature value
 *   2. [AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS] - hex encoding of the binary signature value
 *   3. [AwsSignatureType.HTTP_REQUEST_CHUNK] (SIGV4) - hex encoding of the binary signature value
 *   4. [AwsSignatureType.HTTP_REQUEST_CHUNK] (SIGV4_ASYMMETRIC) - '*'-padded hex encoding of the binary signature value
 *   5. [AwsSignatureType.HTTP_REQUEST_EVENT] - binary signature value (NYI)
 *
 */
public data class SigningResult<T>(val output: T, val signature: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SigningResult<*>

        if (output != other.output) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = output?.hashCode() ?: 0
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

/**
 * Sign [HttpRequest] using the given signing [config]
 *
 * @param request the HTTP request to sign
 * @param config the signing configuration to use
 * @return the signing result
 */
public suspend fun sign(request: HttpRequest, config: AwsSigningConfig): SigningResult<HttpRequest> {
    val crtRequest = request.toSignableCrtRequest()
    val crtResult = AwsSigner.sign(crtRequest, config.toCrt())
    val crtSignedRequest = checkNotNull(crtResult.signedRequest) { "Signed request unexpectedly null" }
    val builder = request.toBuilder()
    builder.update(crtSignedRequest)
    val output = builder.build()
    return SigningResult(output, crtResult.signature)
}
