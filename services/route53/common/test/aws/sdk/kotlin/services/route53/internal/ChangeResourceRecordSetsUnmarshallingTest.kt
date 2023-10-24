/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53.internal

import aws.sdk.kotlin.services.route53.model.InvalidChangeBatch
import aws.sdk.kotlin.services.route53.model.Route53Exception
import aws.sdk.kotlin.services.route53.serde.ChangeResourceRecordSetsOperationDeserializer
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChangeResourceRecordSetsUnmarshallingTest {
    @Test
    fun invalidChangeBatchMessage() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <Messages>
                <Message>InvalidChangeBatch message</Message>
              </Messages>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode.BadRequest,
            Headers.Empty,
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val call = HttpCall(HttpRequestBuilder().build(), response)

        val exception = assertFailsWith<InvalidChangeBatch> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), call)
            }
        }
        assertEquals(listOf<String>("InvalidChangeBatch message"), exception.messages)
    }

    @Test
    fun invalidChangeBatchMessage2() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <Messages>
                <Message>InvalidChangeBatch message 1</Message>
                <Message>InvalidChangeBatch message 2</Message>
              </Messages>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode.BadRequest,
            Headers.Empty,
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val call = HttpCall(HttpRequestBuilder().build(), response)

        val exception = assertFailsWith<InvalidChangeBatch> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), call)
            }
        }
        assertEquals(listOf<String>("InvalidChangeBatch message 1", "InvalidChangeBatch message 2"), exception.messages)
    }

    @Test
    fun invalidChangeBatchMessage3() {
        val bodyText = """
            <?xml version="1.0" encoding="UTF-8"?>
            <InvalidChangeBatch xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
              <Messages>
                <Message>InvalidChangeBatch message 1</Message>
                <Message>InvalidChangeBatch message 2</Message>
              </Messages>
              <Message>InvalidChangeBatch message 3</Message>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </InvalidChangeBatch>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode.BadRequest,
            Headers.Empty,
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val call = HttpCall(HttpRequestBuilder().build(), response)

        val exception = assertFailsWith<InvalidChangeBatch> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), call)
            }
        }
        assertEquals(listOf<String>("InvalidChangeBatch message 1", "InvalidChangeBatch message 2"), exception.messages)
        assertEquals("InvalidChangeBatch message 3", exception.message)
    }

    @Test
    fun changeResourceRecordSetsError() {
        val bodyText = """
            <?xml version="1.0"?>
            <ErrorResponse xmlns="http://route53.amazonaws.com/doc/2016-09-07/">
              <Error>
                <Type>Sender</Type>
                <Code>MalformedXML</Code>
                <Message>ChangeResourceRecordSets error message</Message>
              </Error>
              <RequestId>b25f48e8-84fd-11e6-80d9-574e0c4664cb</RequestId>
            </ErrorResponse>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode.BadRequest,
            Headers.Empty,
            HttpBody.fromBytes(bodyText.encodeToByteArray()),
        )

        val call = HttpCall(HttpRequestBuilder().build(), response)

        val exception = assertFailsWith<Route53Exception> {
            runBlocking {
                ChangeResourceRecordSetsOperationDeserializer().deserialize(ExecutionContext(), call)
            }
        }
        assertEquals("ChangeResourceRecordSets error message", exception.message)
    }
}
