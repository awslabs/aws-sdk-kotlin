/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.service.s3.internal

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
 * Http feature that inspects S3 responses and throws the appropriate modeled service error that matches
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to
 * see if one of the registered errors matches
 */
internal class S3ErrorFeature(private val registry: ExceptionRegistry) : Feature {
    private val emptyByteArray: ByteArray = ByteArray(0)

    internal data class S3Error(
        val requestId: String?,
        val requestId2: String?,
        val code: String?,
        val message: String?
    )

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

    public companion object Feature : HttpClientFeatureFactory<Config, S3ErrorFeature> {
        override val key: FeatureKey<S3ErrorFeature> = FeatureKey("RestXmlError")
        override fun create(block: Config.() -> Unit): S3ErrorFeature {
            val config = Config().apply(block)
            return S3ErrorFeature(config.registry)
        }
    }

    override fun <I, O> install(operation: SdkHttpOperation<I, O>) {
        operation.execution.receive.intercept { req, next ->
            val call = next.call(req)
            val httpResponse = call.response

            val context = req.context
            val expectedStatus = context.getOrNull(HttpOperationContext.ExpectedHttpStatus)?.let { HttpStatusCode.fromValue(it) }
            // TODO: Consider implementing detecting 200-but-error scenarios here.
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
            val modeledExceptionDeserializer = registry[errorResponse?.code]?.deserializer
            val modeledException = modeledExceptionDeserializer?.deserialize(req.context, wrappedResponse) ?: UnknownServiceErrorException(errorResponse?.message)
            setAseFields(modeledException, wrappedResponse, errorResponse)

            // this should never happen...
            val ex = modeledException as? Throwable ?: throw ClientException("registered deserializer for modeled error did not produce an instance of Throwable")
            throw ex
        }
    }

    /**
     * pull the ase specific details from the response / error
     */
    private fun setAseFields(exception: Any, response: HttpResponse, errorDetails: S3Error?) {
        if (exception is AwsServiceException) {
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorCode, errorDetails?.code)
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.ErrorMessage, errorDetails?.message)
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, response.headers[X_AMZN_REQUEST_ID_HEADER])
            exception.sdkErrorMetadata.attributes.setIfNotNull(AwsErrorMetadata.RequestId, errorDetails?.requestId)
            exception.sdkErrorMetadata.attributes[ServiceErrorMetadata.ProtocolResponse] = response
        }
        if (exception is S3Exception) {
            exception.sdkErrorMetadata.attributes.setIfNotNull(S3ErrorMetadata.RequestId2, response.headers["x-amz-id-2"])
            exception.sdkErrorMetadata.attributes.setIfNotNull(S3ErrorMetadata.RequestId2, errorDetails?.requestId2)
        }
    }

    private fun <T : Any> Attributes.setIfNotNull(key: AttributeKey<T>, value: T?) {
        if (value != null) set(key, value)
    }
}

internal suspend fun parseErrorResponse(payload: ByteArray): S3ErrorFeature.S3Error? {
    val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
    val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    val HOSTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("HostId"))
    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Error"))
        field(MESSAGE_DESCRIPTOR)
        field(CODE_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
        field(HOSTID_DESCRIPTOR)
    }

    var message: String? = null
    var code: String? = null
    var requestId: String? = null
    var requestId2: String? = null

    return try {
        val deserializer = XmlDeserializer(payload, true)
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

        S3ErrorFeature.S3Error(requestId, requestId2, code, message)
    } catch (e: DeserializationException) {
        null // return so an appropriate exception type can be instantiated above here.
    }
}
