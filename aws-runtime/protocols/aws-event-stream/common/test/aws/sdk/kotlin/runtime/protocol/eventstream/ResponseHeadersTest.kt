/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import kotlin.test.*

class ResponseHeadersTest {

    @Test
    fun testNormalMessage() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":event-type", HeaderValue.String("Foo"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val actual = message.type()
        assertIs<MessageType.Event>(actual)
        assertEquals("Foo", actual.shapeType)
        assertEquals("application/json", actual.contentType)
    }

    @Test
    fun testExceptionMessage() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("exception"))
            addHeader(":exception-type", HeaderValue.String("BadRequestException"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val actual = message.type()
        assertIs<MessageType.Exception>(actual)
        assertEquals("BadRequestException", actual.shapeType)
        assertEquals("application/json", actual.contentType)
    }

    @Test
    fun testMissingExceptionType() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("exception"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val ex = assertFailsWith<IllegalStateException> {
            message.type()
        }

        assertEquals(ex.message, "Invalid `exception` message: `:exception-type` header is missing")
    }

    @Test
    fun testMissingEventType() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val ex = assertFailsWith<IllegalStateException> {
            message.type()
        }

        assertEquals(ex.message, "Invalid `event` message: `:event-type` header is missing")
    }

    @Test
    fun testMissingMessageType() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":event-type", HeaderValue.String("Foo"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val ex = assertFailsWith<IllegalStateException> {
            message.type()
        }

        assertEquals(ex.message, "`:message-type` header is required to deserialize an event stream message")
    }

    @Test
    fun testMissingContentType() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":event-type", HeaderValue.String("Foo"))
        }

        val actual = message.type()
        assertIs<MessageType.Event>(actual)
        assertEquals("Foo", actual.shapeType)
        assertNull(actual.contentType)
    }

    @Test
    fun testErrorMessage() {
        val message = buildMessage {
            addHeader(":message-type", HeaderValue.String("error"))
            addHeader(":error-code", HeaderValue.String("InternalError"))
            addHeader(":error-message", HeaderValue.String("An internal server error occurred"))
        }

        val actual = message.type()
        assertIs<MessageType.Error>(actual)
        assertEquals("InternalError", actual.errorCode)
        assertEquals("An internal server error occurred", actual.message)
    }

    @Test
    fun testUnknown() {
        val message = buildMessage {
            addHeader(":message-type", HeaderValue.String("foo"))
        }

        val actual = message.type()
        assertIs<MessageType.SdkUnknown>(actual)
        assertEquals("foo", actual.messageType)
    }
}
