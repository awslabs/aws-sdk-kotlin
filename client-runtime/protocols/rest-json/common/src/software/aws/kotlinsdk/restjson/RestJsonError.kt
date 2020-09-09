/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
class RestJsonError(private val registry: ExceptionRegistry) : Feature {
    class Config {
        internal val registry = ExceptionRegistry()

        /**
         * Register a modeled service exception for the given [code]. The deserializer registered MUST provide
         * an [AwsServiceException] when invoked.
         */
        fun register(code: String, deserializer: HttpDeserialize, httpStatusCode: Int? = null) {
            registry.register(ExceptionMetadata(code, deserializer, httpStatusCode?.let { HttpStatusCode.fromValue(it) }))
        }
    }

    companion object Feature : HttpClientFeatureFactory<Config, RestJsonError> {
        override val key: FeatureKey<RestJsonError> = FeatureKey("RestJsonError")
        override fun create(block: Config.() -> Unit): RestJsonError {
            val config = Config().apply(block)
            return RestJsonError(config.registry)
        }
    }

    override fun install(client: SdkHttpClient) {
        // intercept at first chance we get
        client.responsePipeline.intercept(HttpResponsePipeline.Receive) {
            // TODO - we should probable register the success code as part of the user context and check against that instead; otherwise we won't know if we should really throw an error or not
            if (context.response.status.isSuccess()) return@intercept

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
                // TODO - add protocol response
            }

            // this should never happen...
            val ex = modeledException as? Throwable ?: throw ClientException("registered deserializer for modeled error did not produce an exception instance")
            throw ex
        }
    }
}
