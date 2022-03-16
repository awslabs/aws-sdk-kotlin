/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyValue
import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.crt.toSignableCrtRequest
import aws.smithy.kotlin.runtime.crt.update
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.operation.*
import aws.smithy.kotlin.runtime.util.get
import kotlin.time.Duration

/**
 * HTTP request pipeline middleware that signs outgoing requests
 */
@InternalSdkApi
public class AwsSigV4SigningMiddleware(private val config: Config) : ModifyRequestMiddleware {

    public companion object {
        public inline operator fun invoke(block: Config.() -> Unit): AwsSigV4SigningMiddleware = AwsSigV4SigningMiddleware(Config().apply(block))
    }

    public class Config {
        /**
         * The credentials provider used to sign requests with
         */
        public var credentialsProvider: CredentialsProvider? = null

        /**
         * The credential scope service name to sign requests for
         * NOTE: The operation context is favored when [AuthAttributes.SigningService] is set
         */
        public var signingService: String? = null

        /**
         * Sets what signature should be computed
         */
        public var signatureType: AwsSignatureType = AwsSignatureType.HTTP_REQUEST_VIA_HEADERS

        /**
         * The algorithm to sign with
         */
        public var algorithm: AwsSigningAlgorithm = AwsSigningAlgorithm.SIGV4

        /**
         * The uri is assumed to be encoded once in preparation for transmission.  Certain services
         * do not decode before checking signature, requiring double-encoding the uri in the canonical
         * request in order to pass a signature check.
         */
        public var useDoubleUriEncode: Boolean = true

        /**
         * Controls whether or not the uri paths should be normalized when building the canonical request
         */
        public var normalizeUriPath: Boolean = true

        /**
         * Flag indicating if the "X-Amz-Security-Token" query param should be omitted.
         * Normally, this parameter is added during signing if the credentials have a session token.
         * The only known case where this should be true is when signing a websocket handshake to IoT Core.
         */
        public var omitSessionToken: Boolean = false

        /**
         * Optional string to use as the canonical request's body public value.
         * If string is empty, a public value will be calculated from the payload during signing.
         * Typically, this is the SHA-256 of the (request/chunk/event) payload, written as lowercase hex.
         * If this has been precalculated, it can be set here. Special public values used by certain services can also be set
         * (e.g. "UNSIGNED-PAYLOAD" "STREAMING-AWS4-HMAC-SHA256-PAYLOAD" "STREAMING-AWS4-HMAC-SHA256-EVENTS").
         */
        public val signedBodyValue: String? = null

        /**
         * Controls what body "hash" header, if any, should be added to the canonical request and the signed request.
         * Most services do not require this additional header.
         */
        public var signedBodyHeaderType: AwsSignedBodyHeaderType = AwsSignedBodyHeaderType.NONE

        /**
         * If non-zero and the signing transform is query param, then signing will add X-Amz-Expires to the query
         * string, equal to the value specified here.  If this value is zero or if header signing is being used then
         * this parameter has no effect.
         */
        public var expiresAfter: Duration? = null
    }

    override fun install(op: SdkHttpOperation<*, *>) {
        op.execution.finalize.register(this)
    }

    override suspend fun modifyRequest(req: SdkHttpRequest): SdkHttpRequest {
        val credentialsProvider = checkNotNull(config.credentialsProvider)
        val resolvedCredentials = credentialsProvider.getCredentials()
        val logger = req.context.getLogger("AwsSigv4SigningMiddleware")

        val isUnsignedRequest = req.context.isUnsignedRequest()
        // FIXME - an alternative here would be to just pre-compute the sha256 of the payload ourselves and set
        //         the signed body value on the signing config. This would prevent needing to launch a coroutine
        //         for streaming requests since we already have a suspend context.
        val signableRequest = req.subject.toSignableCrtRequest(isUnsignedRequest)

        // SDKs are supposed to default to signed payload _always_ when possible (and when `unsignedPayload` trait isn't present).
        //
        // There are a few escape hatches/special cases:
        //     1. Customer explicitly disables signed payload (via AuthAttributes.UnsignedPayload)
        //     2. Customer provides a (potentially) unbounded stream (via HttpBody.Streaming)
        //
        // When an unbounded stream (2) is given we proceed as follows:
        //     2.1. is it replayable?
        //          (2.1.1) yes -> sign the payload (stream can be consumed more than once)
        //          (2.1.2) no -> unsigned payload
        //
        // NOTE: Chunked signing is NOT enabled through this middleware.
        // NOTE: 2.1.2 is handled below

        // FIXME - see: https://github.com/awslabs/smithy-kotlin/issues/296
        // if we know we have a (streaming) body and toSignableRequest() fails to convert it to a CRT equivalent
        // then we must decide how to compute the payload hash ourselves (defaults to unsigned payload)
        val isUnboundedStream = signableRequest.body == null && req.subject.body is HttpBody.Streaming

        // favor attributes from the current request context
        val precomputedSignedBodyValue = req.context.getOrNull(AuthAttributes.SignedBodyValue)
        val precomputedHeaderType = req.context.getOrNull(AuthAttributes.SignedBodyHeaderType)?.let { AwsSignedBodyHeaderType.valueOf(it) }

        // operation signing config is baseConfig + operation specific config/overrides
        val opSigningConfig = AwsSigningConfig {
            region = req.context[AuthAttributes.SigningRegion]
            service = req.context.getOrNull(AuthAttributes.SigningService) ?: checkNotNull(config.signingService)
            credentials = resolvedCredentials
            algorithm = config.algorithm
            date = req.context.getOrNull(AuthAttributes.SigningDate)

            signatureType = config.signatureType
            omitSessionToken = config.omitSessionToken
            normalizeUriPath = config.normalizeUriPath
            useDoubleUriEncode = config.useDoubleUriEncode
            expiresAfter = config.expiresAfter

            signedBodyHeader = precomputedHeaderType ?: config.signedBodyHeaderType
            signedBodyValue = when {
                precomputedSignedBodyValue != null -> precomputedSignedBodyValue
                isUnsignedRequest -> AwsSignedBodyValue.UNSIGNED_PAYLOAD
                req.subject.body is HttpBody.Empty -> AwsSignedBodyValue.EMPTY_SHA256
                isUnboundedStream -> {
                    logger.warn { "unable to compute hash for unbounded stream; defaulting to unsigned payload" }
                    AwsSignedBodyValue.UNSIGNED_PAYLOAD
                }
                // use the payload to compute the hash
                else -> null
            }
        }

        val signingResult = AwsSigner.sign(signableRequest, opSigningConfig.toCrt())
        val signedRequest = checkNotNull(signingResult.signedRequest) { "signing result must return a non-null HTTP request" }

        // Add the signature to the request context
        req.context[AuthAttributes.RequestSignature] = signingResult.signature

        req.subject.update(signedRequest)
        req.subject.body.resetStream()

        return req
    }
}

/**
 * Check if the current operation should be signed or not
 */
private fun ExecutionContext.isUnsignedRequest(): Boolean = getOrNull(AuthAttributes.UnsignedPayload) ?: false

private fun HttpBody.resetStream() {
    if (this is HttpBody.Streaming && this.isReplayable) {
        this.reset()
    }
}
