/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.json

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.http.Headers
import kotlin.test.Test
import kotlin.test.assertEquals

class RestJsonErrorDeserializerTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `it deserializes aws restJson error codes`() = runSuspendTest {
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
                append(X_AMZN_ERROR_TYPE_HEADER_NAME, value)
            }

            val actual = RestJsonErrorDeserializer.deserialize(headers, null)
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
            val actual = RestJsonErrorDeserializer.deserialize(headers, contents)
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
            val actual = RestJsonErrorDeserializer.deserialize(headers, contents)
            assertEquals(expected, actual.code)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `it deserializes aws restJson error messages`() = runSuspendTest {
        val expected = "one ring to rule bring them all, and in the darkness bind them"

        // header tests
        val errorHeaders = listOf(X_AMZN_ERROR_MESSAGE_HEADER_NAME, X_AMZN_EVENT_ERROR_MESSAGE_HEADER_NAME)
        for (name in errorHeaders) {
            val headers = Headers {
                append(name, expected)
            }

            val actual = RestJsonErrorDeserializer.deserialize(headers, null)
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
            val actual = RestJsonErrorDeserializer.deserialize(headers, contents)
            assertEquals(expected, actual.message)
        }
    }
}
