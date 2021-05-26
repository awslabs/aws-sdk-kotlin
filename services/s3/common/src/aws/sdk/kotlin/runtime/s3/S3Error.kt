/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.s3

import aws.sdk.kotlin.runtime.*
import aws.sdk.kotlin.runtime.http.*
import aws.sdk.kotlin.services.s3.model.S3ErrorMetadata
import aws.sdk.kotlin.services.s3.model.S3Exception
import software.aws.clientrt.ServiceErrorMetadata
import software.aws.clientrt.http.*
import software.aws.clientrt.http.operation.HttpDeserialize
import software.aws.clientrt.http.operation.HttpOperationContext
import software.aws.clientrt.http.operation.SdkHttpOperation
import software.aws.clientrt.http.response.HttpResponse
import software.aws.clientrt.serde.*
import software.aws.clientrt.serde.xml.XmlDeserializer
import software.aws.clientrt.serde.xml.XmlSerialName
import software.aws.clientrt.util.AttributeKey
import software.aws.clientrt.util.Attributes

/**
 * Http feature that inspects responses and throws the appropriate modeled service error that matches
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to
 * see if one of the registered errors matches
 */
@InternalSdkApi
public class S3Error(private val registry: ExceptionRegistry) : Feature {
    private val emptyByteArray: ByteArray = ByteArray(0)

    internal interface RestXmlErrorDetails {
        val requestId: String?
        val requestId2: String?
        val code: String?
        val message: String?
    }

    // Models "ErrorResponse" type in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
    internal data class XmlErrorResponse(
        val error: XmlError?,
        override val requestId: String? = error?.requestId,
        override val requestId2: String? = error?.requestId2
    ) : RestXmlErrorDetails {
        override val code: String? = error?.code
        override val message: String? = error?.message
    }

    // Models "Error" type in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
    internal data class XmlError(
        override val requestId: String?,
        override val requestId2: String?,
        override val code: String?,
        override val message: String?
    ) : RestXmlErrorDetails

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

    /**
     * Deserializes rest Xml protocol errors as specified by:
     * - Smithy spec: https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
     */
    internal object ErrorResponseDeserializer {
        private val ERROR_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("Error"))
        private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
        private val HOSTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("HostId"))
        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
            trait(XmlSerialName("ErrorResponse"))
            field(ERROR_DESCRIPTOR)
            field(REQUESTID_DESCRIPTOR)
        }

        suspend fun deserialize(deserializer: Deserializer): XmlErrorResponse? {
            var requestId: String? = null
            var requestId2: String? = null
            var xmlError: XmlError? = null

            return try {
                deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                    loop@ while (true) {
                        when (findNextFieldIndex()) {
                            ERROR_DESCRIPTOR.index -> xmlError = XmlErrorDeserializer.deserialize(deserializer)
                            REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                            HOSTID_DESCRIPTOR.index -> requestId2 = deserializeString()
                            null -> break@loop
                            else -> skipValue()
                        }
                    }
                }

                XmlErrorResponse(xmlError, requestId ?: xmlError?.requestId, requestId2 ?: xmlError?.requestId2)
            } catch (e: DeserializationException) {
                null // return so an appropriate exception type can be instantiated above here.
            }
        }
    }

    /**
     * This deserializer is used for both the nested Error node from ErrorResponse as well as the top-level
     * Error node as described in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
     */
    internal object XmlErrorDeserializer {
        private val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
        private val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
        private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
        private val HOSTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("HostId"))
        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
            trait(XmlSerialName("Error"))
            field(MESSAGE_DESCRIPTOR)
            field(CODE_DESCRIPTOR)
            field(REQUESTID_DESCRIPTOR)
        }

        suspend fun deserialize(deserializer: Deserializer): XmlError? {
            var message: String? = null
            var code: String? = null
            var requestId: String? = null
            var requestId2: String? = null

            return try {
                deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                    loop@ while (true) {
                        when (findNextFieldIndex()) {
                            MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                            CODE_DESCRIPTOR.index -> code = deserializeString()
                            REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                            HOSTID_DESCRIPTOR.index -> requestId2 = deserializeString()
                            null -> break@loop
                            else -> skipValue()
                        }
                    }
                }

                XmlError(requestId, requestId2, code, message)
            } catch (e: DeserializationException) {
                null // return so an appropriate exception type can be instantiated above here.
            }
        }
    }

    public companion object Feature : HttpClientFeatureFactory<Config, S3Error> {
        override val key: FeatureKey<S3Error> = FeatureKey("RestXmlError")
        override fun create(block: Config.() -> Unit): S3Error {
            val config = Config().apply(block)
            return S3Error(config.registry)
        }
    }

    internal suspend fun parseErrorResponse(payload: ByteArray): RestXmlErrorDetails =
        ErrorResponseDeserializer.deserialize(XmlDeserializer(payload, true))
            ?: XmlErrorDeserializer.deserialize(XmlDeserializer(payload, true))
            ?: throw DeserializationException("Unable to deserialize RestXml error.")


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

    // Provides the policy of what constitutes a status code match in service response
    @InternalSdkApi
    internal fun HttpStatusCode.matches(expected: HttpStatusCode?): Boolean =
        expected == this || (expected == null && this.isSuccess()) || expected?.category() == this.category()

    /**
     * pull the ase specific details from the response / error
     */
    private fun setAseFields(exception: Any, response: HttpResponse, errorDetails: RestXmlErrorDetails?) {
        if (exception is S3Exception) {
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorCode, errorDetails?.code)
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorMessage, errorDetails?.message)
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, response.headers[X_AMZN_REQUEST_ID_HEADER])
            // KGHW add the following line to a fix PR on main
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, errorDetails?.requestId)
            exception.sdkErrorMetadata.attributes.setIfNotNull(S3ErrorMetadata.RequestId2, response.headers[X_AMZN_REQUEST_ID2_HEADER])
            exception.sdkErrorMetadata.attributes.setIfNotNull(S3ErrorMetadata.RequestId2, errorDetails?.requestId2)
            exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
        }
    }

    private fun <T : Any> Attributes.setIfNotNull(key: AttributeKey<T>, value: T?) {
        if (value != null) set(key, value)
    }
}
