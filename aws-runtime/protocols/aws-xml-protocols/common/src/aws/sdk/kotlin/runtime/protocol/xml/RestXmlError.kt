/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.sdk.kotlin.runtime.*
import aws.sdk.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.ServiceErrorMetadata
import aws.smithy.kotlin.runtime.http.*
import aws.smithy.kotlin.runtime.http.operation.HttpDeserialize
import aws.smithy.kotlin.runtime.http.operation.HttpOperationContext
import aws.smithy.kotlin.runtime.http.operation.SdkHttpOperation
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.util.AttributeKey
import aws.smithy.kotlin.runtime.util.Attributes

/**
 * Http feature that inspects responses and throws the appropriate modeled service error that matches
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to
 * see if one of the registered errors matches
 */
@InternalSdkApi
public class RestXmlError(private val registry: ExceptionRegistry) : Feature {
    private val emptyByteArray: ByteArray = ByteArray(0)

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

    public companion object Feature : HttpClientFeatureFactory<Config, RestXmlError> {
        override val key: FeatureKey<RestXmlError> = FeatureKey("RestXmlError")
        override fun create(block: Config.() -> Unit): RestXmlError {
            val config = Config().apply(block)
            return RestXmlError(config.registry)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        // intercept at first chance we get
        operation.execution.receive.intercept { req, next ->
            val call = next.call(req)
            val httpResponse = call.response

            val context = req.context
            val expectedStatus = context.getOrNull(HttpOperationContext.ExpectedHttpStatus)?.let { HttpStatusCode.fromValue(it) }
            if (httpResponse.status.matches(expectedStatus)) return@intercept call

            val payload = httpResponse.body.readAll()
            val wrappedResponse = httpResponse.withPayload(payload)

            // attempt to match the AWS error code
            val errorResponse = try {
                parseErrorResponse(payload ?: emptyByteArray)
            } catch (ex: Exception) {
                throw UnknownServiceErrorException(
                    "failed to parse response as Xml protocol error",
                    ex
                ).also {
                    setAseFields(it, wrappedResponse, null)
                }
            }

            // we already consumed the response body, wrap it to allow the modeled exception to deserialize
            // any members that may be bound to the document
            val modeledExceptionDeserializer = registry[errorResponse.code]?.deserializer
            val modeledException = modeledExceptionDeserializer?.deserialize(req.context, wrappedResponse) ?: UnknownServiceErrorException(errorResponse.message)
            setAseFields(modeledException, wrappedResponse, errorResponse)

            // this should never happen...
            val ex = modeledException as? Throwable ?: throw ClientException("registered deserializer for modeled error did not produce an instance of Throwable")
            throw ex
        }
    }
}

/**
 * pull the ase specific details from the response / error
 */
private fun setAseFields(exception: Any, response: HttpResponse, errorDetails: RestXmlErrorDetails?) {
    if (exception is AwsServiceException) {
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorCode, errorDetails?.code)
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorMessage, errorDetails?.message)
        exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, response.headers[X_AMZN_REQUEST_ID_HEADER])
        exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
    }
}

private fun <T : Any> Attributes.setIfNotNull(key: AttributeKey<T>, value: T?) {
    if (value != null) set(key, value)
}
