/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.aws.kotlinsdk.auth

import software.amazon.awssdk.kotlin.crt.auth.signing.AwsSigner
import software.amazon.awssdk.kotlin.crt.auth.signing.AwsSigningConfig
import software.aws.clientrt.http.*
import software.aws.clientrt.http.request.HttpRequestPipeline
import software.aws.kotlinsdk.InternalSdkApi
import software.aws.kotlinsdk.crt.toSignableCrtRequest
import software.aws.kotlinsdk.crt.update

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
            if (context.executionContext.isUnsignedRequest()) return@intercept

            // TODO - per/operation credentials provider/credentials vs default? i.e. if present favor attributes else use default from feature config?
            // TODO - signing config???
            val credentials = credentialsProvider.getCredentials()

            // FIXME - this is an area where not having to sign a CRT HTTP request might be useful if we could just wrap our own type
            // otherwise to sign a request we need to convert: builder -> crt kotlin HttpRequest (which underneath converts to aws-c-http message) and back
            val signableRequest = subject.toSignableCrtRequest()
            val signingConfig: AwsSigningConfig = AwsSigningConfig.build { TODO("not implemented") }
            val signedRequest = AwsSigner.signRequest(signableRequest, signingConfig)
            subject.update(signedRequest)
        }
    }
}

/**
 * Check if the current operation should be signed or not
 */
private fun ExecutionContext.isUnsignedRequest(): Boolean = contains(AuthAttributes.UnsignedRequest)
