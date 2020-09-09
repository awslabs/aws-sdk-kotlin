/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.aws.kotlinsdk.restjson

import kotlin.test.Test
import kotlin.test.assertEquals
import software.aws.clientrt.http.Headers
import software.aws.clientrt.http.HttpBody
import software.aws.clientrt.http.HttpStatusCode
import software.aws.clientrt.http.content.ByteArrayContent
import software.aws.clientrt.http.request.HttpRequestBuilder
import software.aws.clientrt.http.response.HttpResponse

class RestJsonErrorDeserializerTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `it deserializes aws restJson error codes`() {
        val tests = listOf(
            "FooError",
            "FooError:http://amazon.com/smithy/com.amazon.smithy.validate/",
            "aws.protocoltests.restjson#FooError",
            "aws.protocoltests.restjson#FooError:http://amazon.com/smithy/com.amazon.smithy.validate/"
        )

        val expected = "FooError"

        // header tests
        for (value in tests) {
            val headers = Headers {
                append(AMZN_ERROR_TYPE_HEADER_NAME, value)
            }

            val resp = HttpResponse(HttpStatusCode.BadRequest, headers, HttpBody.Empty, HttpRequestBuilder().build())
            val actual = RestJsonErrorDeserializer.deserialize(resp, null)
            assertEquals(expected, actual.code)
        }

        // body `code` tests
        for (value in tests) {
            val headers = Headers {}
            val contents = """
                {
                    "foo": "bar",
                    "code": "$value",
                    "baz": "quux"
                }
            """.trimIndent().encodeToByteArray()
            val body = ByteArrayContent(contents)
            val resp = HttpResponse(HttpStatusCode.BadRequest, headers, body, HttpRequestBuilder().build())
            val actual = RestJsonErrorDeserializer.deserialize(resp, contents)
            assertEquals(expected, actual.code)
        }

        // body `__type` tests
        for (value in tests) {
            val headers = Headers {}
            val contents = """
                {
                    "foo": "bar",
                    "__type": "$value",
                    "baz": "quux"
                }
            """.trimIndent().encodeToByteArray()
            val body = ByteArrayContent(contents)
            val resp = HttpResponse(HttpStatusCode.BadRequest, headers, body, HttpRequestBuilder().build())
            val actual = RestJsonErrorDeserializer.deserialize(resp, contents)
            assertEquals(expected, actual.code)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `it deserializes aws restJson error messages`() {
        val expected = "one ring to rule bring them all, and in the darkness bind them"

        // header tests
        val errorHeaders = listOf(X_AMZN_ERROR_MESSAGE_HEADER_NAME, X_AMZN_EVENT_ERROR_MESSAGE_HEADER_NAME)
        for (name in errorHeaders) {
            val headers = Headers {
                append(name, expected)
            }

            val resp = HttpResponse(HttpStatusCode.BadRequest, headers, HttpBody.Empty, HttpRequestBuilder().build())
            val actual = RestJsonErrorDeserializer.deserialize(resp, null)
            assertEquals(expected, actual.message)
        }
        val keys = listOf("message", "Message", "errorMessage")

        // body `message` tests
        for (key in keys) {
            val headers = Headers {}
            val contents = """
                {
                    "foo": "bar",
                    "$key": "$expected",
                    "baz": "quux"
                }
            """.trimIndent().encodeToByteArray()
            val body = ByteArrayContent(contents)
            val resp = HttpResponse(HttpStatusCode.BadRequest, headers, body, HttpRequestBuilder().build())
            val actual = RestJsonErrorDeserializer.deserialize(resp, contents)
            assertEquals(expected, actual.message)
        }
    }
}
