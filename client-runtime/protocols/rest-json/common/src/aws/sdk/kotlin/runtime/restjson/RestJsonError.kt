/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.restjson

import aws.sdk.kotlin.runtime.AwsServiceException
import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.runtime.UnknownServiceErrorException
import aws.sdk.kotlin.runtime.http.ExceptionMetadata
import aws.sdk.kotlin.runtime.http.ExceptionRegistry
import aws.sdk.kotlin.runtime.http.X_AMZN_REQUEST_ID_HEADER
import aws.sdk.kotlin.runtime.http.withPayload
import software.aws.clientrt.http.*
import software.aws.clientrt.http.operation.HttpDeserialize
import software.aws.clientrt.http.operation.HttpOperationContext
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.util.InternalAPI

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
        public fun register(code: String, deserializer: HttpDeserialize<*>, httpStatusCode: Int? = null) {
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

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        // intercept at first chance we get
        operation.execution.receive.intercept { req, next ->
            val httpResponse = next.call(req)

            val context = req.context
            val expectedStatus = context.getOrNull(HttpOperationContext.ExpectedHttpStatus)?.let { HttpStatusCode.fromValue(it) }
            if (httpResponse.status.matches(expectedStatus)) return@intercept httpResponse

            val payload = httpResponse.body.readAll()
            val wrappedResponse = httpResponse.withPayload(payload)

            // attempt to match the AWS error code
            val error: RestJsonErrorDetails

            try {
                error = RestJsonErrorDeserializer.deserialize(httpResponse, payload)
            } catch (ex: Exception) {
                throw UnknownServiceErrorException(
                    "failed to parse response as restJson protocol error",
                    ex
                ).also {
                    setAseFields(it, wrappedResponse, null)
                }
            }

            // we already consumed the response body, wrap it to allow the modeled exception to deserialize
            // any members that may be bound to the document
            val deserializer = registry[error.code]?.deserializer
            val modeledException = deserializer?.deserialize(wrappedResponse) ?: UnknownServiceErrorException()
            setAseFields(modeledException, wrappedResponse, error)

            // this should never happen...
            val ex = modeledException as? Throwable ?: throw ClientException("registered deserializer for modeled error did not produce an instance of Throwable")
            throw ex
        }
    }
}

// Provides the policy of what constitutes a status code match in service response
internal fun HttpStatusCode.matches(expected: HttpStatusCode?): Boolean =
    expected == this || (expected == null && this.isSuccess()) || expected?.category() == this.category()

/**
 * pull the ase specific details from the response / error
 */
private fun setAseFields(exception: Any, response: HttpResponse, error: RestJsonErrorDetails?) {
    if (exception is AwsServiceException) {
        exception.requestId = response.headers[X_AMZN_REQUEST_ID_HEADER] ?: ""
        exception.errorCode = error?.code ?: ""
        exception.errorMessage = error?.message ?: ""
        exception.protocolResponse = response
    }
}
