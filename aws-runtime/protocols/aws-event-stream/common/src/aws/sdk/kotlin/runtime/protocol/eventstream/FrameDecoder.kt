/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.ClientException
import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Exception thrown when deserializing raw event stream messages off the wire fails for some reason
 */
public class EventStreamFramingException(message: String, cause: Throwable? = null) : ClientException(message, cause)

/**
 * Convert the raw bytes coming off [chan] to a stream of messages
 */
@InternalSdkApi
public suspend fun decodeFrames(chan: SdkByteReadChannel): Flow<Message> = flow {
    while (!chan.isClosedForRead) {
        // get the prelude to figure out how much is left to read of the message
        val preludeBytes = ByteArray(PRELUDE_BYTE_LEN_WITH_CRC)

        try {
            chan.readFully(preludeBytes)
        } catch (ex: Exception) {
            throw EventStreamFramingException("failed to read message prelude from channel", ex)
        }

        val preludeBuf = SdkByteBuffer.of(preludeBytes).apply { advance(preludeBytes.size.toULong()) }
        val prelude = Prelude.decode(preludeBuf)

        // get a buffer with one complete message in it, prelude has already been read though, leave room for it
        val messageBytes = ByteArray(prelude.totalLen)

        try {
            chan.readFully(messageBytes, offset = PRELUDE_BYTE_LEN_WITH_CRC)
        } catch (ex: Exception) {
            throw EventStreamFramingException("failed to read message from channel", ex)
        }

        val messageBuf = SdkByteBuffer.of(messageBytes)
        messageBuf.writeFully(preludeBytes)
        val remaining = prelude.totalLen - PRELUDE_BYTE_LEN_WITH_CRC
        messageBuf.advance(remaining.toULong())

        val message = Message.decode(messageBuf)
        emit(message)
    }
}
