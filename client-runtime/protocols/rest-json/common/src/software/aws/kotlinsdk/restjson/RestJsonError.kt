/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.aws.kotlinsdk.restjson

import software.aws.clientrt.http.*
import software.aws.clientrt.http.feature.HttpDeserialize
import software.aws.clientrt.http.response.HttpResponsePipeline
import software.aws.clientrt.serde.json.JsonSerdeProvider
import software.aws.clientrt.util.InternalAPI
import software.aws.kotlinsdk.AwsServiceException
import software.aws.kotlinsdk.ClientException
import software.aws.kotlinsdk.UnknownServiceException
import software.aws.kotlinsdk.http.*

/**
 * Http feature that inspects responses and throws the appropriate modeled service error that matches
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to
 * see if one of the registered errors matches
 */
@InternalAPI
public class RestJsonError(private val registry: ExceptionRegistry) : Feature {
    public class Config {
        internal val registry = ExceptionRegistry()

        /**
         * Register a modeled service exception for the given [code]. The deserializer registered MUST provide
         * an [AwsServiceException] when invoked.
         */
        public fun register(code: String, deserializer: HttpDeserialize, httpStatusCode: Int? = null) {
            registry.register(ExceptionMetadata(code, deserializer, httpStatusCode?.let { HttpStatusCode.fromValue(it) }))
        }
    }

    public companion object Feature : HttpClientFeatureFactory<Config, RestJsonError> {
        override val key: FeatureKey<RestJsonError> = FeatureKey("RestJsonError")
        override fun create(block: Config.() -> Unit): RestJsonError {
            val config = Config().apply(block)
            return RestJsonError(config.registry)
        }
    }

    override fun install(client: SdkHttpClient) {
        // intercept at first chance we get
        client.responsePipeline.intercept(HttpResponsePipeline.Receive) {
            val expectedStatus = context.executionContext.getOrNull(SdkOperation.ExpectedHttpStatus)?.let { HttpStatusCode.fromValue(it) }
            val status = context.response.status
            if (status.matches(expectedStatus)) return@intercept

            val payload = context.response.body.readAll()

            // attempt to match the AWS error code
            val error = RestJsonErrorDeserializer.deserialize(context.response, payload)

            val provider = JsonSerdeProvider()

            // we already consumed the response body, wrap it to allow the modeled exception to deserialize
            // any members that may be bound to the document
            val wrappedResponse = context.response.withPayload(payload)
            val deserializer = registry[error.code]?.deserializer
            val modeledException = deserializer?.deserialize(wrappedResponse, provider::deserializer) ?: UnknownServiceException()
            if (modeledException is AwsServiceException) {
                // set ase specific details
                modeledException.requestId = wrappedResponse.headers[X_AMZN_REQUEST_ID_HEADER] ?: ""
                modeledException.errorCode = error.code ?: ""
                modeledException.errorMessage = error.message ?: ""
                modeledException.protocolResponse = wrappedResponse
            }

            // this should never happen...
            val ex = modeledException as? Throwable ?: throw ClientException("registered deserializer for modeled error did not produce an instance of Throwable")
            throw ex
        }
    }
}

// Provides the policy of what constitutes a status code match in service response
internal fun HttpStatusCode.matches(expected: HttpStatusCode?): Boolean =
    expected == this || (expected == null && this.isSuccess()) || expected?.category() == this.category()
