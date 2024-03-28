/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.serde.GetBucketLocationOperationDeserializer
import aws.smithy.kotlin.runtime.http.Headers
import aws.smithy.kotlin.runtime.http.HttpBody
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.request.HttpRequestBuilder
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import aws.smithy.kotlin.runtime.time.Instant
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetBucketLocationOperationDeserializerTest {
    @Test
    fun deserializeUnwrappedResponse() {
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/">us-west-2</LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode.OK,
            Headers.Empty,
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val call = HttpCall(HttpRequestBuilder().build(), response, Instant.now(), Instant.now())

        val actual = runBlocking {
            GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), call, responseXML.encodeToByteArray())
        }

        assertEquals(BucketLocationConstraint.UsWest2, actual.locationConstraint)
    }

    @Test
    fun deserializeErrorMessage() {
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <Error>
               <Message>Some message</Message>
               <RequestId>Some request ID</RequestId>
            </Error>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode.BadRequest,
            Headers.Empty,
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val call = HttpCall(HttpRequestBuilder().build(), response, Instant.now(), Instant.now())
        val exception = assertFailsWith<S3Exception> {
            runBlocking {
                GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), call, responseXML.encodeToByteArray())
            }
        }

        assertEquals("Some message, Request ID: Some request ID", exception.message)
    }
}
