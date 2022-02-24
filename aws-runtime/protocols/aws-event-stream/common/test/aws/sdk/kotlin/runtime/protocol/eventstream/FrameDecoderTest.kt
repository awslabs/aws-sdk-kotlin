/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.io.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FrameDecoderTest {

    @Test
    fun testFrameStreamSingleMessage() = runTest {
        val encoded = validMessageWithAllHeaders()
        val expected = Message.decode(SdkByteBuffer.wrapAsReadBuffer(encoded))
        val chan = SdkByteReadChannel(encoded)

        val frames = decodeFrames(chan)
        val actual = frames.toList()

        assertEquals(1, actual.size)
        assertEquals(expected, actual.first())
    }

    @Test
    fun testFrameStreamMultipleMessagesChunked() = runTest {
        val encoded = SdkByteBuffer(0u).apply {
            writeFully(validMessageWithAllHeaders())
            writeFully(validMessageEmptyPayload())
            writeFully(validMessageNoHeaders())
        }.bytes()

        val expected1 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageWithAllHeaders()))
        val expected2 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageEmptyPayload()))
        val expected3 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageNoHeaders()))

        val chan = SdkByteReadChannel(encoded)
        val frames = decodeFrames(chan)

        val actual = frames.toList()

        assertEquals(3, actual.size)
        assertEquals(expected1, actual[0])
        assertEquals(expected2, actual[1])
        assertEquals(expected3, actual[2])
    }

    @Ignore
    @Test
    fun testChannelClosed() = runTest {
        TODO("not implemented yet: need to add test for channel closed normally while waiting on prelude")
    }
}
