package aws.sdk.kotlin.services.route53.internal

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.sdk.kotlin.services.route53.model.ChangeResourceRecordSetsResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.*

class CustomErrorUnmarshallingTest {
    @Test
    fun unmarshallChangeResourceRecordSetsInvalidChangeBatchResponse(){
        val deserializer = ChangeResourceRecordSetsOperationDeserializer()
        val status: HttpStatusCode = HttpStatusCode(400, "Bad Request")
        val headers: Headers = Headers.invoke {  }
        val responseText =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<InvalidChangeBatch xmlns=\"https://route53.amazonaws.com/doc/2013-04-01/\">\n" +
                "  <Messages>\n" +
                "    <Message>Tried to create resource record set duplicate.example.com. type A, but it already exists</Message>\n" +
                "  </Messages>\n" +
                "  <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>\n" +
                "</InvalidChangeBatch>"
        val body: HttpBody = HttpBody.fromBytes(responseText.encodeToByteArray())

        val response : HttpResponse = HttpResponse(
            status,
            headers,
            body
        )
        val job = GlobalScope.launch {
            val result = deserializer.deserialize(ExecutionContext(), response)
            println(result)
        }
        runBlocking {
            job.join()
        }
//        val actual = deserializer.deserialize(ExecutionContext(), response)
//        println(actual)
        println("\n\n\n\nTest Test Test\n\n\n\n")
        val expected: ChangeResourceRecordSetsResponse = ChangeResourceRecordSetsResponse.invoke {  } // TODO: See if you can use the constructor
        //assertEquals(expected, actual)
        assertEquals(true, false)
    }

    @Test
    fun exampleTest(){
        assertEquals(true, true)
    }
}
