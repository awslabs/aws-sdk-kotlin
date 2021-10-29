/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResponseHeadersTest {

    @Test
    fun testNormalMessage() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("event"))
            addHeader(":event-type", HeaderValue.String("Foo"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val actual = message.parseResponseHeaders()
        assertEquals("event", actual.messageType)
        assertEquals("Foo", actual.shapeType)
        assertEquals("application/json", actual.contentType)
    }

    @Test
    fun testErrorMessage() {
        val message = buildMessage {
            payload = "test".encodeToByteArray()
            addHeader(":message-type", HeaderValue.String("exception"))
            addHeader(":exception-type", HeaderValue.String("BadRequestException"))
            addHeader(":content-type", HeaderValue.String("application/json"))
        }

        val actual = message.parseResponseHeaders()
        assertEquals("exception", actual.messageType)
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
            message.parseResponseHeaders()
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
            message.parseResponseHeaders()
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
            message.parseResponseHeaders()
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

        val actual = message.parseResponseHeaders()
        assertEquals("event", actual.messageType)
        assertEquals("Foo", actual.shapeType)
        assertNull(actual.contentType)
    }
}
