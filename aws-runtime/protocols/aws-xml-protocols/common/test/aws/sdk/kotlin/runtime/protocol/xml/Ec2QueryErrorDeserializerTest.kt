/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.serde.DeserializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class Ec2QueryErrorDeserializerTest {
    @Test
    fun `it deserializes ec2Query errors`() = runSuspendTest {
        val payload = """
            <Response>
                <Errors>
                    <Error>
                        <Code>InvalidGreeting</Code>
                        <Message>Hi</Message>
                        <AnotherSetting>Foo</AnotherSetting>
                    </Error>
                </Errors>
                <RequestId>foo-request</RequestId>
            </Response>
        """.trimIndent().encodeToByteArray()
        val actual = parseEc2QueryErrorResponse(payload)
        assertEquals("InvalidGreeting", actual.code)
        assertEquals("Hi", actual.message)
        assertEquals("foo-request", actual.requestId)
    }

    @Test
    fun `it fails to deserialize invalid ec2Query errors`() = runSuspendTest {
        val tests = listOf(
            """
                <SomeRandomNode>
                    <Errors>
                        <Error>
                            <Code>InvalidGreeting</Code>
                            <Message>Hi</Message>
                            <AnotherSetting>Foo</AnotherSetting>
                        </Error>
                    </Errors>
                    <RequestId>foo-request</RequestId>
                </SomeRandomNode>
            """,
            """
                <Error>
                    <Code>InvalidGreeting</Code>
                    <Message>Hi</Message>
                    <RequestId>foo-request</RequestId>
                </Error>
            """,
            """
                Utter garbage! 🤪
            """,
        ).map { it.trimIndent().encodeToByteArray() }

        for (payload in tests) {
            assertFailsWith<DeserializationException> {
                parseEc2QueryErrorResponse(payload)
            }
        }
    }

    @Test
    fun `it partially deserializes ec2Query errors`() = runSuspendTest {
        val tests = listOf(
            """
                <Response>
                    <SomeRandomNode>
                        <Error>
                            <Code>InvalidGreeting</Code>
                            <Message>Hi</Message>
                            <AnotherSetting>Foo</AnotherSetting>
                        </Error>
                    </SomeRandomNode>
                    <RequestId>foo-request</RequestId>
                </Response>
            """,
            """
                <Response>
                    <Errors />
                    <RequestId>foo-request</RequestId>
                </Response>
            """,
        ).map { it.trimIndent().encodeToByteArray() }

        for (payload in tests) {
            val actual = parseEc2QueryErrorResponse(payload)
            assertNull(actual.code)
            assertNull(actual.message)
            assertEquals("foo-request", actual.requestId)
        }
    }
}
