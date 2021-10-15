/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.signing

import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyValue
import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProvider
import aws.sdk.kotlin.runtime.crt.toSignableCrtRequest
import aws.sdk.kotlin.runtime.crt.update
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.operation.withContext
import aws.smithy.kotlin.runtime.logging.Logger
import aws.smithy.kotlin.runtime.util.get

/**
 * HTTP request pipeline middleware that signs outgoing requests
 */
@InternalSdkApi
public class AwsSigV4SigningMiddleware internal constructor(private val config: Config) : Feature {

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
         * Controls what body "hash" header, if any, should be added to the canonical request and the signed request.
         * Most services do not require this additional header.
         */
        public var signedBodyHeaderType: AwsSignedBodyHeaderType = AwsSignedBodyHeaderType.NONE
    }

    public companion object Feature : HttpClientFeatureFactory<Config, AwsSigV4SigningMiddleware> {
        private val logger = Logger.getLogger<AwsSigV4SigningMiddleware>()

        override val key: FeatureKey<AwsSigV4SigningMiddleware> = FeatureKey("AwsSigv4SigningMiddleware")

        override fun create(block: Config.() -> Unit): AwsSigV4SigningMiddleware {
            val config = Config().apply(block)

            requireNotNull(config.credentialsProvider) { "AwsSigv4SigningMiddleware requires a credentialsProvider" }
            requireNotNull(config.signingService) { "AwsSigv4SigningMiddleware requires a signing service" }

            return AwsSigV4SigningMiddleware(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.finalize.intercept { req, next ->

            val credentialsProvider = checkNotNull(config.credentialsProvider)
            val resolvedCredentials = credentialsProvider.getCredentials()
            val logger: Logger by lazy { logger.withContext(req.context) }

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

                signedBodyHeader = config.signedBodyHeaderType
                signedBodyValue = when {
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

            val signedRequest = AwsSigner.signRequest(signableRequest, opSigningConfig.toCrt())
            req.subject.update(signedRequest)
            req.subject.body.resetStream()

            next.call(req)
        }
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
