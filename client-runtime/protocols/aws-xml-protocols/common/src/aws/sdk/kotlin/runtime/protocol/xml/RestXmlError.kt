/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.sdk.kotlin.runtime.*
import aws.sdk.kotlin.runtime.http.ExceptionMetadata
import aws.sdk.kotlin.runtime.http.ExceptionRegistry
import aws.sdk.kotlin.runtime.http.X_AMZN_REQUEST_ID_HEADER
import aws.sdk.kotlin.runtime.http.withPayload
import software.aws.clientrt.ServiceErrorMetadata
import software.aws.clientrt.http.*
import software.aws.clientrt.http.operation.HttpDeserialize
import software.aws.clientrt.http.operation.HttpOperationContext
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.util.AttributeKey
import software.aws.clientrt.util.Attributes

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
                context.parseErrorResponse(payload ?: emptyByteArray)
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

// Provides the policy of what constitutes a status code match in service response
@InternalSdkApi
internal fun HttpStatusCode.matches(expected: HttpStatusCode?): Boolean =
    expected == this || (expected == null && this.isSuccess()) || expected?.category() == this.category()

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
