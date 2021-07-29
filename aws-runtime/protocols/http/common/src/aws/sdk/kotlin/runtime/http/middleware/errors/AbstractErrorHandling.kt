/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.http.middleware.errors

import aws.sdk.kotlin.runtime.*
import aws.sdk.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.operation.HttpDeserialize
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.util.setIfNotNull

/**
 * Common error response details
 */
@InternalSdkApi
public data class ErrorDetails(val code: String?, val message: String?, val requestId: String?)

/**
 * Http feature that inspects responses and throws the appropriate modeled service error.
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to see if one of
 * the registered errors matches.
 */
@InternalSdkApi
public abstract class AbstractErrorHandling(private val registry: ExceptionRegistry) : Feature {
    protected val emptyByteArray: ByteArray = ByteArray(0)
    protected abstract val protocolName: String

    public class Config {
        public var registry: ExceptionRegistry = ExceptionRegistry()

        /**
         * Register a modeled service exception for the given [code]. The deserializer registered MUST provide
         * an [AwsServiceException] when invoked.
         */
        public fun register(code: String, deserializer: HttpDeserialize<*>, httpStatusCode: Int? = null) {
            registry.register(ExceptionMetadata(code, deserializer, httpStatusCode?.let { HttpStatusCode.fromValue(it) }))
        }
    }

    public abstract class AbstractFeature<T : Feature> : HttpClientFeatureFactory<Config, T> {
        final override fun create(block: Config.() -> Unit): T = create(Config().apply(block))
        protected abstract fun create(config: Config): T
    }

    final override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        // intercept at first chance we get
        operation.execution.receive.intercept { req, next ->
            val call = next.call(req)
            val httpResponse = call.response

            // FIXME - deal with ExpectedHttpStatus
            val context = req.context
            val expectedStatus = context
                .getOrNull(HttpOperationContext.ExpectedHttpStatus)
                ?.let(HttpStatusCode::fromValue)
            if (httpResponse.status.matches(expectedStatus)) return@intercept call // No error

            val payload = httpResponse.body.readAll()
            val wrappedResponse = httpResponse.withPayload(payload)

            // attempt to match the AWS error code
            val errorDetails = try {
                parseErrorResponse(httpResponse.headers, payload)
            } catch (ex: Exception) {
                throw UnknownServiceErrorException(
                    "failed to parse response as a $protocolName protocol error",
                    ex
                ).also {
                    setAseFields(it, wrappedResponse, null)
                }
            }

            // we already consumed the response body, wrap it to allow the modeled exception to deserialize
            // any members that may be bound to the document
            val modeledExceptionDeserializer = registry[errorDetails.code]?.deserializer
            val modeledException = modeledExceptionDeserializer
                ?.deserialize(req.context, wrappedResponse)
                ?: UnknownServiceErrorException(errorDetails.message)
            setAseFields(modeledException, wrappedResponse, errorDetails)

            // this should never happen...
            val ex = modeledException as? Throwable ?: throw ClientException("registered deserializer for modeled error did not produce an instance of Throwable")
            throw ex
        }
    }

    protected abstract suspend fun parseErrorResponse(headers: Headers, payload: ByteArray?): ErrorDetails
}

/**
 * pull the ase specific details from the response / error
 */
@InternalSdkApi
public fun setAseFields(exception: Any, response: HttpResponse, errorDetails: ErrorDetails?) {
    if (exception is AwsServiceException) {
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorCode, errorDetails?.code)
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorMessage, errorDetails?.message)
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, response.headers[X_AMZN_REQUEST_ID_HEADER])
        exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
    }
}
