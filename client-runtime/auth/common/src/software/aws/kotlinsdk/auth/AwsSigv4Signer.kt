/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.auth

import software.amazon.awssdk.kotlin.crt.auth.signing.AwsSignedBodyValue
import software.amazon.awssdk.kotlin.crt.auth.signing.AwsSigner
import software.amazon.awssdk.kotlin.crt.auth.signing.AwsSigningAlgorithm
import software.amazon.awssdk.kotlin.crt.auth.signing.AwsSigningConfig
import software.aws.clientrt.http.*
import software.aws.clientrt.http.request.HttpRequestPipeline
import software.aws.clientrt.time.epochMilliseconds
import software.aws.clientrt.util.get
import software.aws.kotlinsdk.InternalSdkApi
import software.aws.kotlinsdk.crt.toSignableCrtRequest
import software.aws.kotlinsdk.crt.update
import software.amazon.awssdk.kotlin.crt.auth.credentials.Credentials as CredentialsCrt

/**
 * HTTP request pipeline middleware that signs outgoing requests
 */
@InternalSdkApi
public class AwsSigv4Signer internal constructor(config: Config) : Feature {
    private val credentialsProvider = requireNotNull(config.credentialsProvider) { "AwsSigv4Signer requires a credentialsProvider" }

    public class Config {
        public var credentialsProvider: CredentialsProvider? = null
    }

    public companion object Feature : HttpClientFeatureFactory<Config, AwsSigv4Signer> {
        override val key: FeatureKey<AwsSigv4Signer> = FeatureKey("Signer")

        override fun create(block: Config.() -> Unit): AwsSigv4Signer {
            val config = Config().apply(block)
            return AwsSigv4Signer(config)
        }
    }

    override fun install(client: SdkHttpClient) {
        client.requestPipeline.intercept(HttpRequestPipeline.Finalize) {
            val resolvedCredentials = credentialsProvider.getCredentials()

            // FIXME - this is an area where not having to sign a CRT HTTP request might be useful if we could just wrap our own type
            // otherwise to sign a request we need to convert: builder -> crt kotlin HttpRequest (which underneath converts to aws-c-http message) and back
            val signableRequest = subject.toSignableCrtRequest()

            val signingConfig: AwsSigningConfig = AwsSigningConfig.build {
                region = context.executionContext[AuthAttributes.SigningRegion]
                service = context.executionContext[SdkOperation.ServiceName]
                credentials = resolvedCredentials.toCrt()
                algorithm = AwsSigningAlgorithm.SIGV4
                date = context.executionContext.getOrNull(AuthAttributes.SigningDate)?.epochMilliseconds

                if (context.executionContext.isUnsignedRequest()) {
                    this.signedBodyValue = AwsSignedBodyValue.UNSIGNED_PAYLOAD
                }

                // TODO - expose additional signing config as needed as context attributes?
            }
            val signedRequest = AwsSigner.signRequest(signableRequest, signingConfig)
            subject.update(signedRequest)
        }
    }
}

/**
 * Check if the current operation should be signed or not
 */
private fun ExecutionContext.isUnsignedRequest(): Boolean = getOrNull(AuthAttributes.UnsignedPayload) ?: false

private fun Credentials.toCrt(): CredentialsCrt = CredentialsCrt(accessKeyId, secretAccessKey, sessionToken)
