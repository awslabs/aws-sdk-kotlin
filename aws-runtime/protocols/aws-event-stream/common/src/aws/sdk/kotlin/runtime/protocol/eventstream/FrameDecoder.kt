/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
        // null indicates the channel was closed and that no more messages are coming
        val messageBuf = readPrelude(chan) ?: return@flow
        val prelude = Prelude.decode(messageBuf.peek())
        val limit = prelude.totalLen - PRELUDE_BYTE_LEN_WITH_CRC

        try {
            chan.readFully(messageBuf, limit.toLong())
        } catch (ex: Exception) {
            throw EventStreamFramingException("failed to read message from channel", ex)
        }

        val message = Message.decode(messageBuf)
        emit(message)
    }
}

/**
 * Read the message prelude from the channel.
 * @return prelude bytes or null if the channel is closed and no additional prelude is coming
 */
private suspend fun readPrelude(chan: SdkByteReadChannel): SdkBuffer? {
    val dest = SdkBuffer()
    var remaining = PRELUDE_BYTE_LEN_WITH_CRC.toLong()
    while (remaining > 0 && !chan.isClosedForRead) {
        val rc = chan.read(dest, remaining)
        if (rc == -1L) break
        remaining -= rc
    }

    // 0 bytes read and channel closed indicates no messages remaining -> null
    if (remaining == PRELUDE_BYTE_LEN_WITH_CRC.toLong() && chan.isClosedForRead) return null

    // partial read -> failure
    if (remaining > 0) throw EventStreamFramingException("failed to read event stream message prelude from channel: read: ${dest.size} bytes, expected $remaining more bytes")

    return dest
}
