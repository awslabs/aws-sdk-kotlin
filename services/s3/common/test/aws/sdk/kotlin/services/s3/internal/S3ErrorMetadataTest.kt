/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.s3.internal

import kotlin.test.*

class S3ErrorDeserializerTest {

    @Test
    fun `it deserializes s3 errors`() {
        val payload = """
            <Error>
                <Type>Sender</Type>
                <Code>InvalidGreeting</Code>
                <Message>Hi</Message>
                <AnotherSetting>setting</AnotherSetting>
                <RequestId>foo-id</RequestId>
                <HostId>bar-id</HostId>
            </Error>
        """.trimIndent().encodeToByteArray()

        val actual = parseS3ErrorResponse(payload)
        assertNotNull(actual)
        assertEquals("InvalidGreeting", actual.code)
        assertEquals("Hi", actual.message)
        assertEquals("foo-id", actual.requestId)
        assertEquals("bar-id", actual.requestId2)
    }

    @Test
    fun `it partially deserializes s3 errors`() {
        val tests = listOf(
            """
                <Error>
                    <RequestId>foo-id</RequestId>
                </Error>
            """.trimIndent().encodeToByteArray(),
        )

        for (payload in tests) {
            val error = parseS3ErrorResponse(payload)
            assertNotNull(error)
            assertEquals("foo-id", error.requestId)
            assertNull(error.code)
            assertNull(error.message)
        }
    }
}
