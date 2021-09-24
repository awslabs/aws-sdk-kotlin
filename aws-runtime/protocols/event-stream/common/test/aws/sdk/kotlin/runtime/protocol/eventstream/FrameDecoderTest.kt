/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.writeByte
import aws.smithy.kotlin.runtime.io.writeFully
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class FrameDecoderTest {
    @Test
    fun testSingleStreamingMessage() {
        val encoded = validMessageWithAllHeaders()

        val decoder = FrameDecoder()
        val buf = SdkByteBuffer(256u)
        for (i in 0 until encoded.size - 1) {
            buf.writeByte(encoded[i])
            assertEquals(DecodedFrame.Incomplete, decoder.decodeFrame(buf), "incomplete frame shouldn't result in a message")
        }

        buf.writeByte(encoded.last())

        when (val frame = decoder.decodeFrame(buf)) {
            is DecodedFrame.Incomplete -> fail("frame should be complete now")
            is DecodedFrame.Complete -> {
                val expected = Message.decode(SdkByteBuffer.wrapAsReadBuffer(encoded))
                assertEquals(expected, frame.message)
            }
        }
    }

    @Test
    fun testMultipleStreamingMessagesChunked() {
        val encoded = SdkByteBuffer(256u).apply {
            writeFully(validMessageWithAllHeaders())
            writeFully(validMessageEmptyPayload())
            writeFully(validMessageNoHeaders())
        }

        val decoder = FrameDecoder()
        val chunkSize = 8

        val totalChunks = encoded.readRemaining / chunkSize.toULong()
        val buffer = SdkByteBuffer(256u)
        val decoded = mutableListOf<Message>()
        for (i in 0..totalChunks.toInt()) {
            buffer.writeFully(encoded, min(chunkSize.toULong(), encoded.readRemaining))
            when (val frame = decoder.decodeFrame(buffer)) {
                is DecodedFrame.Incomplete -> {}
                is DecodedFrame.Complete -> decoded.add(frame.message)
            }
        }

        val expected1 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageWithAllHeaders()))
        val expected2 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageEmptyPayload()))
        val expected3 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageNoHeaders()))
        assertEquals(3, decoded.size)
        assertEquals(expected1, decoded[0])
        assertEquals(expected2, decoded[1])
        assertEquals(expected3, decoded[2])
    }
}
