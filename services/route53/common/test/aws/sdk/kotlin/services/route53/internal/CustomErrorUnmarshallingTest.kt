package aws.sdk.kotlin.services.route53.internal

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.sdk.kotlin.services.route53.model.ChangeResourceRecordSetsResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import aws.sdk.kotlin.services.route53.model.Route53Exception

class CustomErrorUnmarshallingTest {
    @Test
    fun unmarshallChangeResourceRecordSetsInvaildChangeBatchResponse(){
        val bodyText =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<InvalidChangeBatch xmlns=\"https://route53.amazonaws.com/doc/2013-04-01/\">\n" +
            "  <Messages>\n" +
            "    <Message>This is a ChangeResourceRecordSets InvaildChangeBatchResponse message</Message>\n" +
            "  </Messages>\n" +
            "  <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>\n" +
            "</InvalidChangeBatch>"

        val response : HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke {  },
            HttpBody.fromBytes(bodyText.encodeToByteArray())
        )

        val exception = assertThrows<Route53Exception> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("This is a ChangeResourceRecordSets InvaildChangeBatchResponse message", exception.message)
    }

    @Test
    fun unmarshallChangeResourceRecordSetsGenericErrorResponse(){
        val bodyText =
            "<?xml version=\"1.0\"?>\n" +
            "<ErrorResponse xmlns=\"http://route53.amazonaws.com/doc/2016-09-07/\">\n" +
            "  <Error>\n" +
            "    <Type>Sender</Type>\n" +
            "    <Code>MalformedXML</Code>\n" +
            "    <Message>This is a ChangeResourceRecordSets generic error message</Message>\n" +
            "  </Error>\n" +
            "  <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>\n" +
            "</ErrorResponse>"

        val response : HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke {  },
            HttpBody.fromBytes(bodyText.encodeToByteArray())
        )

        val exception = assertThrows<Route53Exception> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("This is a ChangeResourceRecordSets generic error message", exception.message)
    }
}
