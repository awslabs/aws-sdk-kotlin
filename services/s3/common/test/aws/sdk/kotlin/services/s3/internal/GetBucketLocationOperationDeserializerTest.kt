/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.services.s3.internal

import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.sdk.kotlin.services.s3.transform.GetBucketLocationOperationDeserializer
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
    fun deserializeUnwrappedResponse() {
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/">us-west-2</LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(200, "Success"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val actual = runBlocking {
            GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
        }

        assertEquals("UsWest2", actual.locationConstraint.toString())
    }

    @Test
    fun deserializeWrappedResponse() {
        val responseXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <LocationConstraint>
               <LocationConstraint xmlns="http://s3.amazonaws.com/doc/2006-03-01/">us-west-2</LocationConstraint>
            </LocationConstraint>
        """.trimIndent()

        val response: HttpResponse = HttpResponse(
            HttpStatusCode(200, "Success"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val actual = runBlocking {
            GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
        }

        assertEquals("UsWest2", actual.locationConstraint.toString())
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
            HttpStatusCode(400, "Bad Request"),
            Headers.invoke { },
            HttpBody.fromBytes(responseXML.encodeToByteArray()),
        )

        val exception = assertThrows<S3Exception> {
            runBlocking {
                GetBucketLocationOperationDeserializer().deserialize(ExecutionContext(), response)
            }
        }

        assertEquals("Some message", exception.message)
    }
}
