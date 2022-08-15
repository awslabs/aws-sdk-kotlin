/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FrameEncoderTest {
    @Test
    fun testEncode() = runTest {
        val expected = listOf(
            validMessageWithAllHeaders(),
            validMessageEmptyPayload(),
            validMessageNoHeaders(),
        )

        val message1 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageWithAllHeaders()))
        val message2 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageEmptyPayload()))
        val message3 = Message.decode(SdkByteBuffer.wrapAsReadBuffer(validMessageNoHeaders()))

        val messages = flowOf(
            message1,
            message2,
            message3,
        )

        val actual = messages.encode().toList()

        assertEquals(3, actual.size)
        assertContentEquals(expected[0], actual[0])
        assertContentEquals(expected[1], actual[1])
        assertContentEquals(expected[2], actual[2])
    }

    @Test
    fun testAsEventStreamHttpBody() = runTest {
        val messages = flowOf(
            "foo",
            "bar",
            "baz",
        ).map { it.encodeToByteArray() }

        val body = messages.asEventStreamHttpBody()
        val actual = body.readAll()
        val expected = "foobarbaz"
        assertEquals(expected, actual?.decodeToString())
    }
}
