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

// Operation deserializer from ChangeResourceRecordSetsOperationDeserializer
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
        "InvalidChangeBatch" -> InvalidChangeBatchDeserializer().deserialize(context, wrappedResponse)
        "InvalidInput" -> InvalidInputDeserializer().deserialize(context, wrappedResponse)
        "NoSuchHealthCheck" -> NoSuchHealthCheckDeserializer().deserialize(context, wrappedResponse)
        "NoSuchHostedZone" -> NoSuchHostedZoneDeserializer().deserialize(context, wrappedResponse)
        "PriorRequestNotComplete" -> PriorRequestNotCompleteDeserializer().deserialize(context, wrappedResponse)
        else -> Route53Exception(errorDetails.message)
    }

    setAseErrorMetadata(ex, wrappedResponse, errorDetails)
    throw ex
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

// XML Error response parser from RestXMLErrorDeserializer
/**
 * Provides access to specific values regardless of message form
 */
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

/**
 * Deserializes rest XML protocol errors as specified by:
 * https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#error-response-serialization
 *
 * Returns parsed data in normalized form or throws IllegalArgumentException if response cannot be parsed.
 * NOTE: we use an explicit XML deserializer here because we rely on validating the root element name
 * for dealing with the alternate error response forms
 */
@InternalApi
public suspend fun parseRestXmlErrorResponse(payload: ByteArray): ErrorDetails {
    val details = ErrorResponseDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: XmlErrorDeserializer.deserialize(XmlDeserializer(payload, true))
        ?: throw DeserializationException("Unable to deserialize RestXml error.")
    return ErrorDetails(details.code, details.message, details.requestId)
}

internal object ErrorResponseDeserializer {
    private val MESSAGES_DESCRIPTOR = SdkFieldDescriptor(SerialKind.Struct, XmlSerialName("Messages"))
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("InvalidChangeBatch"))
        field(MESSAGES_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlErrorResponse? {
        var requestId: String? = null
        var xmlError: XmlError? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGES_DESCRIPTOR.index -> xmlError = XmlErrorDeserializer.deserialize(deserializer)
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
    private val REQUESTID_DESCRIPTOR = SdkFieldDescriptor(SerialKind.String, XmlSerialName("RequestId"))
    private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build {
        trait(XmlSerialName("Error"))
        field(MESSAGE_DESCRIPTOR)
        field(REQUESTID_DESCRIPTOR)
    }

    suspend fun deserialize(deserializer: Deserializer): XmlError? {
        var message: String? = null
        var requestId: String? = null

        return try {
            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        MESSAGE_DESCRIPTOR.index -> message = deserializeString()
                        REQUESTID_DESCRIPTOR.index -> requestId = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            XmlError(requestId, null, message)
        } catch (e: DeserializationException) {
            null // return so an appropriate exception type can be instantiated above here.
        }
    }
}
