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
import auth.credentials.CredentialUtils
import auth.signer.internal.AbstractAws4Signer
import auth.signer.internal.Aws4SignerRequestParams
import auth.signer.internal.AwsChunkedEncodingInputStream
import auth.signer.internal.SignerConstant.X_AMZ_CONTENT_SHA256
import auth.signer.params.AwsS3V4SignerParams
import core.exception.SdkClientException
import core.interceptor.ExecutionAttributes
import http.ContentStreamProvider
import http.SdkHttpFullRequest
import utils.BinaryUtils
import java.io.IOException
import kotlin.jvm.Throws

/**
 * AWS4 signer implementation for AWS S3
 */
@SdkPublicApi
class AwsS3V4Signer private constructor() : AbstractAws4Signer<AwsS3V4SignerParams?, Aws4PresignerParams?>() {
    fun sign(request: SdkHttpFullRequest?, executionAttributes: ExecutionAttributes): SdkHttpFullRequest {
        val signingParams: AwsS3V4SignerParams = constructAwsS3SignerParams(executionAttributes)
        return sign(request, signingParams)
    }

    /**
     * A method to sign the given #request. The parameters required for signing are provided through the modeled
     * [AwsS3V4Signer] class.
     *
     * @param request The request to sign
     * @param signingParams Class with the parameters used for signing the request
     * @return A signed version of the input request
     */
    fun sign(request: SdkHttpFullRequest, signingParams: AwsS3V4SignerParams): SdkHttpFullRequest {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request
        }
        val requestParams = Aws4SignerRequestParams(signingParams)
        return doSign(request, requestParams, signingParams).build()
    }

    private fun constructAwsS3SignerParams(executionAttributes: ExecutionAttributes): AwsS3V4SignerParams {
        val signerParams: AwsS3V4SignerParams.Builder = extractSignerParams(
            AwsS3V4SignerParams.builder(),
            executionAttributes
        )
        java.util.Optional
            .ofNullable(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_CHUNKED_ENCODING))
            .ifPresent(signerParams::enableChunkedEncoding)
        java.util.Optional
            .ofNullable(executionAttributes.getAttribute(S3SignerExecutionAttribute.ENABLE_PAYLOAD_SIGNING))
            .ifPresent(signerParams::enablePayloadSigning)
        return signerParams.build()
    }

    fun presign(request: SdkHttpFullRequest?, executionAttributes: ExecutionAttributes?): SdkHttpFullRequest {
        val signingParams: Aws4PresignerParams =
            extractPresignerParams(Aws4PresignerParams.builder(), executionAttributes).build()
        return presign(request, signingParams)
    }

    /**
     * A method to pre sign the given #request. The parameters required for pre signing are provided through the modeled
     * [Aws4PresignerParams] class.
     *
     * @param request The request to pre-sign
     * @param signingParams Class with the parameters used for pre signing the request
     * @return A pre signed version of the input request
     */
    fun presign(request: SdkHttpFullRequest, signingParams: Aws4PresignerParams): SdkHttpFullRequest {
        // anonymous credentials, don't sign
        if (CredentialUtils.isAnonymous(signingParams.awsCredentials())) {
            return request
        }
        val requestParams = Aws4SignerRequestParams(signingParams)
        return doPresign(request, requestParams, signingParams).build()
    }

    /**
     * If necessary, creates a chunk-encoding wrapper on the request payload.
     */
    protected fun processRequestPayload(
        mutableRequest: SdkHttpFullRequest.Builder,
        signature: ByteArray,
        signingKey: ByteArray,
        signerRequestParams: Aws4SignerRequestParams?,
        signerParams: AwsS3V4SignerParams
    ) {
        if (useChunkEncoding(mutableRequest, signerParams)) {
            if (mutableRequest.contentStreamProvider() != null) {
                val streamProvider: ContentStreamProvider = mutableRequest.contentStreamProvider()
                mutableRequest.contentStreamProvider({
                    asChunkEncodedStream(
                        streamProvider.newStream(),
                        signature,
                        signingKey,
                        signerRequestParams
                    )
                })
            }
        }
    }

    protected fun calculateContentHashPresign(
        mutableRequest: SdkHttpFullRequest.Builder?,
        signerParams: Aws4PresignerParams?
    ): String {
        return UNSIGNED_PAYLOAD
    }

    private fun asChunkEncodedStream(
        inputStream: java.io.InputStream,
        signature: ByteArray,
        signingKey: ByteArray,
        signerRequestParams: Aws4SignerRequestParams
    ): AwsChunkedEncodingInputStream {
        return AwsChunkedEncodingInputStream(
            inputStream,
            signingKey,
            signerRequestParams.getFormattedRequestSigningDateTime(),
            signerRequestParams.getScope(),
            BinaryUtils.toHex(signature), this
        )
    }

    /**
     * Returns the pre-defined header value and set other necessary headers if
     * the request needs to be chunk-encoded. Otherwise calls the superclass
     * method which calculates the hash of the whole content for signing.
     */
    protected fun calculateContentHash(
        mutableRequest: SdkHttpFullRequest.Builder,
        signerParams: AwsS3V4SignerParams
    ): String {
        // To be consistent with other service clients using sig-v4,
        // we just set the header as "required", and AWS4Signer.sign() will be
        // notified to pick up the header value returned by this method.
        mutableRequest.putHeader(X_AMZ_CONTENT_SHA256, "required")
        return if (isPayloadSigningEnabled(mutableRequest, signerParams)) {
            if (useChunkEncoding(mutableRequest, signerParams)) {
                val contentLength: String =
                    mutableRequest.firstMatchingHeader(CONTENT_LENGTH)
                        .orElse(null)
                val originalContentLength: Long
                originalContentLength = contentLength?.toLong()
                    ?:
                            /**
                             * "Content-Length" header could be missing if the caller is
                             * uploading a stream without setting Content-Length in
                             * ObjectMetadata. Before using sigv4, we rely on HttpClient to
                             * add this header by using BufferedHttpEntity when creating the
                             * HttpRequest object. But now, we need this information
                             * immediately for the signing process, so we have to cache the
                             * stream here.
                             */
                            try {
                                getContentLength(mutableRequest)
                            } catch (e: IOException) {
                                throw SdkClientException.builder()
                                    .message("Cannot get the content-length of the request content.")
                                    .cause(e)
                                    .build()
                            }
                mutableRequest.putHeader("x-amz-decoded-content-length", java.lang.Long.toString(originalContentLength))
                // Make sure "Content-Length" header is not empty so that HttpClient
                // won't cache the stream again to recover Content-Length
                mutableRequest.putHeader(
                    CONTENT_LENGTH, java.lang.Long.toString(
                        AwsChunkedEncodingInputStream.calculateStreamContentLength(originalContentLength)
                    )
                )
                CONTENT_SHA_256
            } else {
                super.calculateContentHash(mutableRequest, signerParams)
            }
        } else UNSIGNED_PAYLOAD
    }

    /**
     * Determine whether to use aws-chunked for signing
     */
    private fun useChunkEncoding(
        mutableRequest: SdkHttpFullRequest.Builder,
        signerParams: AwsS3V4SignerParams
    ): Boolean {
        // Chunked encoding only makes sense to do when the payload is signed
        return isPayloadSigningEnabled(mutableRequest, signerParams) && isChunkedEncodingEnabled(signerParams)
    }

    /**
     * @return True if chunked encoding has been enabled. Otherwise false.
     */
    private fun isChunkedEncodingEnabled(signerParams: AwsS3V4SignerParams): Boolean {
        val isChunkedEncodingEnabled: Boolean = signerParams.enableChunkedEncoding()
        return isChunkedEncodingEnabled != null && isChunkedEncodingEnabled
    }

    /**
     * @return True if payload signing is explicitly enabled.
     */
    private fun isPayloadSigningEnabled(
        request: SdkHttpFullRequest.Builder,
        signerParams: AwsS3V4SignerParams
    ): Boolean {
        /**
         * If we aren't using https we should always sign the payload unless there is no payload
         */
        if (!request.protocol().equals("https") && request.contentStreamProvider() != null) {
            return true
        }
        val isPayloadSigningEnabled: Boolean = signerParams.enablePayloadSigning()
        return isPayloadSigningEnabled != null && isPayloadSigningEnabled
    }

    companion object {
        private const val CONTENT_SHA_256 = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD"

        /**
         * Sent to S3 in lieu of a payload hash when unsigned payloads are enabled
         */
        private const val UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD"
        private const val CONTENT_LENGTH = "Content-Length"
        fun create(): AwsS3V4Signer {
            return AwsS3V4Signer()
        }

        /**
         * Read the content of the request to get the length of the stream.
         */
        @Throws(IOException::class)
        private fun getContentLength(requestBuilder: SdkHttpFullRequest.Builder): Long {
            val content: java.io.InputStream = requestBuilder.contentStreamProvider().newStream()
            var contentLength: Long = 0
            val tmp = ByteArray(4096)
            var read: Int
            while (content.read(tmp).also({ read = it }) != -1) {
                contentLength += read.toLong()
            }
            return contentLength
        }
    }
}