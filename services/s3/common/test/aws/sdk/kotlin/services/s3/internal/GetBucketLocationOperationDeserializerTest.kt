package aws.sdk.kotlin.services.s3.internal

import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetBucketLocationOperationDeserializerTest {
    @Test
    fun deserializeUnwrappedResponse(){
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/">us-west-2</LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val actual = runBlocking {
            GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
        }

        assertEquals("UsWest2", actual.locationConstraint.toString())
    }

    @Test
    fun deserializeWrappedResponse(){
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint>
               <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/">us-west-2</LocationConstraint>
            </LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val actual = runBlocking {
            GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
        }

        assertEquals("UsWest2", actual.locationConstraint.toString())
    }

    @Test
    fun deserializeUnwrappedResponseMissingLocation(){
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/"></LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("Did not receive a value for 'LocationConstraint' in response.", exception.message)
    }

    @Test
    fun deserializeWrappedResponseMissingLocation(){
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint>
               <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/"></LocationConstraint>
            </LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }

        assertEquals("Did not receive a value for 'LocationConstraint' in response.", exception.message)
    }


}