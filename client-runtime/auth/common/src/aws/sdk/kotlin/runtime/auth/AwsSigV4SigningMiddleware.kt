/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.auth.signing.AwsSignedBodyValue
import aws.sdk.kotlin.crt.auth.signing.AwsSigner
import aws.sdk.kotlin.crt.auth.signing.AwsSigningAlgorithm
import aws.sdk.kotlin.crt.auth.signing.AwsSigningConfig
import aws.sdk.kotlin.crt.toSignableCrtRequest
import aws.sdk.kotlin.crt.update
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.execution.AuthAttributes
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.*
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.operation.logger
import software.aws.clientrt.time.epochMilliseconds
import software.aws.clientrt.util.get

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
        override val key: FeatureKey<AwsSigV4SigningMiddleware> = FeatureKey("AwsSigv4SigningMiddleware")

        override fun create(block: Config.() -> Unit): AwsSigV4SigningMiddleware {
            val config = Config().apply(block)

            requireNotNull(config.credentialsProvider) { "AwsSigv4Signer requires a credentialsProvider" }
            requireNotNull(config.signingService) { "AwsSigv4Signer requires a signing service" }

            return AwsSigV4SigningMiddleware(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.finalize.intercept { req, next ->

            val credentialsProvider = checkNotNull(config.credentialsProvider)
            val resolvedCredentials = credentialsProvider.getCredentials()

            // FIXME - this is an area where not having to sign a CRT HTTP request might be useful if we could just wrap our own type
            // otherwise to sign a request we need to convert: builder -> crt kotlin HttpRequest (which underneath converts to aws-c-http message) and back
            val signableRequest = req.subject.toSignableCrtRequest()

            // SDKs are supposed to default to signed payload _always_ when possible (and when `unsignedPayload` trait isn't present).
            //
            // There are a few escape hatches/special cases:
            //     1. Customer explicitly disables signed payload (via AuthAttributes.UnsignedPayload)
            //     2. Customer provides a (potentially) unbounded stream (via HttpBody.Streaming)
            //
            // When an unbounded stream (2) is given we proceed as follows:
            //     2.1. is it a file?
            //          (2.1.1) yes -> sign the payload (bounded stream/special case)
            //          (2.1.2) no -> unsigned payload
            //
            // NOTE: Chunked signing is NOT enabled through this middleware.
            // NOTE:
            //     2.1.1 is handled by toSignableRequest() by special casing file inputs
            //     2.1.2 is handled below
            //

            // FIXME - see: https://github.com/awslabs/smithy-kotlin/issues/296
            // if we know we have a (streaming) body and toSignableRequest() fails to convert it to a CRT equivalent
            // then we must decide how to compute the payload hash ourselves (defaults to unsigned payload)
            val isUnboundedStream = signableRequest.body == null && req.subject.body is HttpBody.Streaming

            val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
                region = req.context[AuthAttributes.SigningRegion]
                service = req.context.getOrNull(AuthAttributes.SigningService) ?: checkNotNull(config.signingService)
                credentials = resolvedCredentials.toCrt()
                algorithm = AwsSigningAlgorithm.SIGV4
                date = req.context.getOrNull(AuthAttributes.SigningDate)?.epochMilliseconds

                signatureType = config.signatureType.toCrt()
                omitSessionToken = config.omitSessionToken
                normalizeUriPath = config.normalizeUriPath
                useDoubleUriEncode = config.useDoubleUriEncode

                signedBodyHeader = config.signedBodyHeaderType.toCrt()
                signedBodyValue = when {
                    req.context.isUnsignedRequest() -> AwsSignedBodyValue.UNSIGNED_PAYLOAD
                    req.subject.body is HttpBody.Empty -> AwsSignedBodyValue.EMPTY_SHA256
                    isUnboundedStream -> {
                        req.context.logger.warn { "unable to compute hash for unbounded stream; defaulting to unsigned payload" }
                        AwsSignedBodyValue.UNSIGNED_PAYLOAD
                    }
                    // use the payload to compute the hash
                    else -> null
                }

                // TODO - expose additional signing config as needed as context attributes? Would allow per/operation override of some of these settings potentially
            }
            val signedRequest = AwsSigner.signRequest(signableRequest, signingConfig)
            req.subject.update(signedRequest)

            next.call(req)
        }
    }
}

/**
 * Check if the current operation should be signed or not
 */
private fun ExecutionContext.isUnsignedRequest(): Boolean = getOrNull(AuthAttributes.UnsignedPayload) ?: false
