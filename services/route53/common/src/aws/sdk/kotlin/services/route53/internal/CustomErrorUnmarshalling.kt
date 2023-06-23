package aws.sdk.kotlin.services.route53.internal

import aws.sdk.kotlin.services.route53.model.ChangeResourceRecordSetsResponse
import aws.sdk.kotlin.services.route53.model.Route53Exception
import aws.sdk.kotlin.services.route53.transform.*
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.awsprotocol.ErrorDetails
import aws.smithy.kotlin.runtime.awsprotocol.setAseErrorMetadata
import aws.smithy.kotlin.runtime.awsprotocol.withPayload
import aws.smithy.kotlin.runtime.http.isSuccess
import aws.smithy.kotlin.runtime.http.operation.HttpDeserialize
import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.serde.*
import aws.smithy.kotlin.runtime.serde.xml.XmlDeserializer
import aws.smithy.kotlin.runtime.serde.xml.XmlNamespace
import aws.smithy.kotlin.runtime.serde.xml.XmlSerialName

internal class ChangeResourceRecordSetsOperationDeserializer : HttpDeserialize<ChangeResourceRecordSetsResponse> {
    override suspend fun deserialize(context: ExecutionContext, response: HttpResponse): ChangeResourceRecordSetsResponse {
        if (!response.status.isSuccess()) {
            throwChangeResourceRecordSetsError(context, response)
        }

        val builder = ChangeResourceRecordSetsResponse.Builder()
        val payload = response.body.readAll()
        if (payload != null) {
            deserializeChangeResourceRecordSetsOperationBody(builder, payload)
        }
        return builder.build()
    }
}

private fun deserializeChangeResourceRecordSetsOperationBody(builder: ChangeResourceRecordSetsResponse.Builder, payload: ByteArray) {
    val deserializer = XmlDeserializer(payload)
    val CHANGEINFO_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("ChangeInfo"))
    val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("ChangeResourceRecordSetsResponse"))
        trait(XmlNamespace("https://route53.amazonaws.com/doc/2013-04-01/"))
        field(CHANGEINFO_DESCRIPTOR)
    }

    deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
        loop@while (true) {
            when (findNextFieldIndex()) {
                CHANGEINFO_DESCRIPTOR.index -> builder.changeInfo = deserializeChangeInfoDocument(deserializer)
                null -> break@loop
                else -> skipValue()
            }
        }
    }
}

private suspend fun throwChangeResourceRecordSetsError(context: ExecutionContext, response: HttpResponse): kotlin.Nothing {
    val payload = response.body.readAll()
    val wrappedResponse = response.withPayload(payload)

    val errorDetails = try {
        checkNotNull(payload) { "unable to parse error from empty response" }
        parseRestXmlErrorResponse(payload)
    } catch (ex: Exception) {
        throw Route53Exception("Failed to parse response as 'restXml' error", ex).also {
            setAseErrorMetadata(it, wrappedResponse, null)
        }
    }

    val ex = when (errorDetails.code) {
        "InvalidInput" -> InvalidInputDeserializer().deserialize(context, wrappedResponse)
        "NoSuchHealthCheck" -> NoSuchHealthCheckDeserializer().deserialize(context, wrappedResponse)
        "NoSuchHostedZone" -> NoSuchHostedZoneDeserializer().deserialize(context, wrappedResponse)
        "PriorRequestNotComplete" -> PriorRequestNotCompleteDeserializer().deserialize(context, wrappedResponse)
        else -> Route53Exception(errorDetails.message)
    }

    setAseErrorMetadata(ex, wrappedResponse, errorDetails)
    throw ex
}

@InternalApi
public suspend fun parseRestXmlErrorResponse(payload: ByteArray): ErrorDetails {
    val details = ErrorResponseDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: XmlErrorDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: InvalidChangeBatchDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: InvalidChangeBatchMessageDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: throw DeserializationException("Unable to deserialize RestXml error.")
    return ErrorDetails(details.code, details.message, details.requestId)
}

internal object ErrorResponseDeserializer {
    private val ERROR_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("Error"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("ErrorResponse"))
        field(ERROR_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlErrorResponse? {
        var requestId: String? = null
        var xmlError: XmlError? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        ERROR_DESCRIPTOR.index -> xmlError = XmlErrorDeserializer.deserialize(deserializer)
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlErrorResponse(xmlError, requestId ?: xmlError?.requestId)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

internal object XmlErrorDeserializer {
    private val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    private val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
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

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                        CODE_DESCRIPTOR.index -> code = deserializeString()
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlError(requestId, code, message)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

internal object InvalidChangeBatchDeserializer {
    private val MESSAGES_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("Messages"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("InvalidChangeBatch"))
        field(MESSAGES_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlErrorResponse? {
        var requestId: String? = null
        var messages: XmlError? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGES_DESCRIPTOR.index -> messages = InvalidChangeBatchMessageDeserializer.deserialize(deserializer)
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlErrorResponse(messages, requestId ?: messages?.requestId)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

internal object InvalidChangeBatchMessageDeserializer {
    private val MESSAGE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Message"))
    private val CODE_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("Code"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Messages"))
        field(MESSAGE_DESCRIPTOR)
        field(CODE_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlError? {
        var message: String? = null
        var code: String? = null
        var requestId: String? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                        CODE_DESCRIPTOR.index -> code = deserializeString()
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlError(requestId, code, message)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}

// XML Error response parser from RestXMLErrorDeserializer
internal interface RestXmlErrorDetails {
    val requestId: String?
    val code: String?
    val message: String?
}

// Models "ErrorResponse" type in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
internal data class XmlErrorResponse(
    val error: XmlError?,
    override val requestId: String? = error?.requestId,
) : RestXmlErrorDetails {
    override val code: String? = error?.code
    override val message: String? = error?.message
}

// Models "Error" type in https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#operation-error-serialization
internal data class XmlError(
    override val requestId: String?,
    override val code: String?,
    override val message: String?,
) : RestXmlErrorDetails
