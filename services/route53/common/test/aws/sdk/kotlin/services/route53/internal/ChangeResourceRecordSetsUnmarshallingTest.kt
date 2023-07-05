/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53.internal

import aws.sdk.kotlin.services.route53.model.InvalidChangeBatch
import aws.sdk.kotlin.services.route53.model.Route53Exception
import aws.sdk.kotlin.services.route53.transform.ChangeResourceRecordSetsOperationDeserializer
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.serde.DeserializationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ChangeResourceRecordSetsUnmarshallingTest {
    @Test
    fun changeResourceRecordSetsInvalidChangeBatchMessage() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <Messages>
                <Message>This is a ChangeResourceRecordSets InvalidChangeBatch response message</Message>
              </Messages>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val exception = assertThrows<InvalidChangeBatch> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("This is a ChangeResourceRecordSets InvalidChangeBatch response message", exception.message)
    }

    @Test
    fun changeResourceRecordSetsInvalidChangeBatchMessages() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <Messages>
                <Message>This is a ChangeResourceRecordSets InvalidChangeBatch response message</Message>
                <Message>This is also a ChangeResourceRecordSets InvalidChangeBatch response message</Message>
              </Messages>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val exception = assertThrows<InvalidChangeBatch> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals(
            "This is a ChangeResourceRecordSets InvalidChangeBatch response message\n" +
                "This is also a ChangeResourceRecordSets InvalidChangeBatch response message",
            exception.message,
        )
    }

    @Test
    fun changeResourceRecordSetsInvalidChangeBatchNoMessage() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <Messages>
              </Messages>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val exception = assertThrows<DeserializationException> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("Missing message in InvalidChangeBatch XML response", exception.message)
    }

    @Test
    fun changeResourceRecordSetsInvalidChangeBatchNoMessages() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val exception = assertThrows<DeserializationException> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("Missing message in InvalidChangeBatch XML response", exception.message)
    }

    @Test
    fun changeResourceRecordSetsGenericError() {
        val bodyText = """
            <?xml version="1.0"?>
            <ErrorResponse xmlns="http://route53.amazonaws.com/doc/2016-09-07/">
              <Error>
                <Type>Sender</Type>
                <Code>MalformedXML</Code>
                <Message>This is a ChangeResourceRecordSets generic error message</Message>
              </Error>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </ErrorResponse>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val exception = assertThrows<Route53Exception> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("This is a ChangeResourceRecordSets generic error message", exception.message)
    }

    @Test
    fun emptyResponse() {
        val response: HttpResponse = HttpResponse(
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes("".encodeToByteArray()),
        )

        val exception = assertThrows<Route53Exception> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }
        assertEquals("Failed to parse response as 'restXml' error", exception.message)
    }
}
