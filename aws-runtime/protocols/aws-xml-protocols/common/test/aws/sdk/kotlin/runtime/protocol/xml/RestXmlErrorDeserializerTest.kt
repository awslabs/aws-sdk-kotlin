/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.smithy.kotlin.runtime.serde.DeserializationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class RestXmlErrorDeserializerTest {

    @Test
    fun `it deserializes aws restXml errors`() = runTest {
        val tests = listOf(
            """
                <ErrorResponse>
                    <Error>
                        <Type>Sender</Type>
                        <Code>InvalidGreeting</Code>
                        <Message>Hi</Message>
                        <AnotherSetting>setting</AnotherSetting>
                    </Error>
                    <RequestId>foo-id</RequestId>
                </ErrorResponse>
            """.trimIndent().encodeToByteArray(),
            """
                <Error>
                    <Type>Sender</Type>
                    <Code>InvalidGreeting</Code>
                    <Message>Hi</Message>
                    <AnotherSetting>setting</AnotherSetting>
                    <RequestId>foo-id</RequestId>
                </Error>
            """.trimIndent().encodeToByteArray()
        )

        for (payload in tests) {
            val actual = parseRestXmlErrorResponse(payload)
            assertEquals("InvalidGreeting", actual.code)
            assertEquals("Hi", actual.message)
            assertEquals("foo-id", actual.requestId)
        }
    }

    @Test
    fun `it fails to deserialize invalid aws restXml errors`() = runTest {
        val tests = listOf(
            """
                <SomeRandomThing>
                    <Error>
                        <Type>Sender</Type>
                        <Code>InvalidGreeting</Code>
                        <Message>Hi</Message>
                        <AnotherSetting>setting</AnotherSetting>
                    </Error>
                    <RequestId>foo-id</RequestId>
                </SomeRandomThing>
            """.trimIndent().encodeToByteArray(),
            """
                <SomeRandomThing>
                    <Type>Sender</Type>
                    <Code>InvalidGreeting</Code>
                    <Message>Hi</Message>
                    <AnotherSetting>setting</AnotherSetting>
                    <RequestId>foo-id</RequestId>
                </SomeRandomThing>
            """.trimIndent().encodeToByteArray()
        )

        for (payload in tests) {
            assertFailsWith<DeserializationException>() {
                parseRestXmlErrorResponse(payload)
            }
        }
    }

    @Test
    fun `it partially deserializes aws restXml errors`() = runTest {
        val tests = listOf(
            """
                <ErrorResponse>
                    <SomeRandomThing>
                        <Type>Sender</Type>
                        <Code>InvalidGreeting</Code>
                        <Message>Hi</Message>
                        <AnotherSetting>setting</AnotherSetting>
                    </SomeRandomThing>
                    <RequestId>foo-id</RequestId>
                </ErrorResponse>
            """.trimIndent().encodeToByteArray()
        )

        for (payload in tests) {
            val error = parseRestXmlErrorResponse(payload)
            assertEquals("foo-id", error.requestId)
            assertNull(error.code)
            assertNull(error.message)
        }
    }
}
