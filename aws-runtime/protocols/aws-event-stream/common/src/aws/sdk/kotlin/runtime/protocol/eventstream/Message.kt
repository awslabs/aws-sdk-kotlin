/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.hashing.crc32
import aws.smithy.kotlin.runtime.io.*

internal const val MESSAGE_CRC_BYTE_LEN = 4

// max message size is 16 MB
internal const val MAX_MESSAGE_SIZE = 16 * 1024 * 1024

// max header size is 128 KB
internal const val MAX_HEADER_SIZE = 128 * 1024

/*
    Message Wire Format
    See also: https://docs.aws.amazon.com/transcribe/latest/dg/event-stream.html

    +--------------------------------------------------------------------+   --
    |                            Total Len (32)                          |     |
    +--------------------------------------------------------------------+     | Prelude
    |                          Headers Len (32)                          |     |
    +--------------------------------------------------------------------+     |
    |                          Prelude CRC (32)                          |     |
    +--------------------------------------------------------------------+   --
    |                            Headers (*)                         ... |
    +--------------------------------------------------------------------+
    |                            Payload (*)                         ... |
    +--------------------------------------------------------------------+
    |                          Message CRC (32)                          |
    +--------------------------------------------------------------------+
*/

/**
 * An event stream message
 */
@InternalSdkApi
public data class Message(val headers: List<Header>, val payload: ByteArray) {

    public companion object {
        /**
         * Read a message from [buffer]
         */
        public fun decode(buffer: Buffer): Message {
            val totalLen = buffer.readUInt()
            check(totalLen <= MAX_MESSAGE_SIZE.toUInt()) { "Invalid Message size: $totalLen" }

            // read into new ByteArray so we can validate the CRC
            val messageBytes = ByteArray(totalLen.toInt() - MESSAGE_CRC_BYTE_LEN)
            val messageBuffer = SdkByteBuffer.of(messageBytes)
            // add back in the totalLen so we can read the prelude
            messageBuffer.writeUInt(totalLen)
            buffer.readFully(messageBuffer)

            val prelude = Prelude.decode(messageBuffer)

            val remaining = prelude.totalLen - PRELUDE_BYTE_LEN_WITH_CRC - MESSAGE_CRC_BYTE_LEN
            check(messageBuffer.readRemaining >= remaining.toULong()) { "Invalid buffer, not enough remaining; have: ${messageBuffer.readRemaining}; expected $remaining" }

            val message = MessageBuilder()

            // read headers
            var headerBytesConsumed = 0UL
            while (headerBytesConsumed < prelude.headersLength.toULong()) {
                val start = messageBuffer.readPosition
                val header = Header.decode(messageBuffer)
                headerBytesConsumed += messageBuffer.readPosition - start
                message.addHeader(header)
            }
            check(headerBytesConsumed == prelude.headersLength.toULong()) { "Invalid Message: expected ${prelude.headersLength} header bytes; consumed $headerBytesConsumed" }

            val payload = ByteArray(prelude.payloadLen)
            messageBuffer.readFully(payload)
            message.payload = payload

            val expectedCrc = buffer.readUInt()
            val computedCrc = messageBytes.crc32()
            check(computedCrc == expectedCrc) {
                "Message checksum mismatch; expected=0x${expectedCrc.toString(16)}; calculated=0x${computedCrc.toString(16)}"
            }
            return message.build()
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Message

        if (headers != other.headers) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = headers.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    /**
     * Encode a message to the [dest] buffer
     */
    public fun encode(dest: MutableBuffer) {
        val encodedHeaders = SdkByteBuffer(16u)
        headers.forEach { it.encode(encodedHeaders) }
        val headersLen = encodedHeaders.readRemaining.toInt()
        val payloadLen = payload.size

        val messageLen = PRELUDE_BYTE_LEN_WITH_CRC + headersLen + payloadLen + MESSAGE_CRC_BYTE_LEN
        check(headersLen < MAX_HEADER_SIZE) { "Invalid Headers length: $headersLen" }
        check(messageLen < MAX_MESSAGE_SIZE) { "Invalid Message length: $messageLen" }

        val prelude = Prelude(messageLen, headersLen)

        val encodedMessage = ByteArray(messageLen - MESSAGE_CRC_BYTE_LEN)
        val messageBuf = SdkByteBuffer.of(encodedMessage)

        prelude.encode(messageBuf)
        messageBuf.writeFully(encodedHeaders)
        messageBuf.writeFully(payload)

        dest.writeFully(encodedMessage)
        dest.writeInt(encodedMessage.crc32().toInt())
    }
}

private fun emptyByteArray(): ByteArray = ByteArray(0)

/**
 * Used to constructing a single event stream [Message]
 */
@InternalSdkApi
public class MessageBuilder {
    public val headers: MutableList<Header> = mutableListOf()
    public var payload: ByteArray? = null

    public fun addHeader(header: Header) { headers.add(header) }
    public fun addHeader(name: String, value: HeaderValue) { headers.add(Header(name, value)) }

    public fun build(): Message = Message(headers, payload ?: emptyByteArray())
}

/**
 * Builds a new [Message] by populating a [MessageBuilder] using the given [block]
 * @return the constructed messsage
 */
@InternalSdkApi
public fun buildMessage(block: MessageBuilder.() -> Unit): Message = MessageBuilder().apply(block).build()
