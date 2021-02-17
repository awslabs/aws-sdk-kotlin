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
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.*
import software.aws.clientrt.time.epochMilliseconds
import software.aws.clientrt.util.get

/**
 * HTTP request pipeline middleware that signs outgoing requests
 */
@InternalSdkApi
public class AwsSigv4Signer internal constructor(config: Config) : Feature {
    private val credentialsProvider = requireNotNull(config.credentialsProvider) { "AwsSigv4Signer requires a credentialsProvider" }
    private val signingService = requireNotNull(config.signingService) { "AwsSigv4Signer requires a signing service" }

    public class Config {
        /**
         * The credentials provider used to sign requests with
         */
        public var credentialsProvider: CredentialsProvider? = null

        /**
         * The credential scope service name to sign requests for
         */
        public var signingService: String? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, AwsSigv4Signer> {
        override val key: FeatureKey<AwsSigv4Signer> = FeatureKey("AwsSigv4Signer")

        override fun create(block: Config.() -> Unit): AwsSigv4Signer {
            val config = Config().apply(block)
            return AwsSigv4Signer(config)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.finalize.intercept { req, next ->

            val resolvedCredentials = credentialsProvider.getCredentials()

            // FIXME - this is an area where not having to sign a CRT HTTP request might be useful if we could just wrap our own type
            // otherwise to sign a request we need to convert: builder -> crt kotlin HttpRequest (which underneath converts to aws-c-http message) and back
            val signableRequest = req.request.toSignableCrtRequest()

            val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
                region = req.context[AuthAttributes.SigningRegion]
                service = req.context.getOrNull(AuthAttributes.SigningService) ?: signingService
                credentials = resolvedCredentials.toCrt()
                algorithm = AwsSigningAlgorithm.SIGV4
                date = req.context.getOrNull(AuthAttributes.SigningDate)?.epochMilliseconds

                if (req.context.isUnsignedRequest()) {
                    this.signedBodyValue = AwsSignedBodyValue.UNSIGNED_PAYLOAD
                }

                // TODO - expose additional signing config as needed as context attributes?
            }
            val signedRequest = AwsSigner.signRequest(signableRequest, signingConfig)
            req.request.update(signedRequest)

            next.call(req)
        }
    }
}

/**
 * Check if the current operation should be signed or not
 */
private fun ExecutionContext.isUnsignedRequest(): Boolean = getOrNull(AuthAttributes.UnsignedPayload) ?: false
