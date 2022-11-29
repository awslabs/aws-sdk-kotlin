/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
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
         * Read a message from [source]
         */
        public fun decode(source: SdkBufferedSource): Message {
            val totalLen = source.peek().use { it.readInt().toUInt() }
            check(totalLen <= MAX_MESSAGE_SIZE.toUInt()) { "Invalid Message size: $totalLen" }

            // Limiting the amount of data read by SdkBufferedSource is tricky and cause incorrect CRC
            // if not careful (e.g. creating a buffered source of CrcSource will usually lead to incorrect results
            // because the entire point SdkBufferedSource (okio.BufferedSource) is to buffer larger chunks internally
            // to optimize short reads)
            val messageBuffer = SdkBuffer()
            val computedCrc = run {
                val crcSource = CrcSource(source)
                crcSource.read(messageBuffer, totalLen.toLong() - MESSAGE_CRC_BYTE_LEN.toLong())
                crcSource.crc
            }

            val prelude = Prelude.decode(messageBuffer)

            val remaining = prelude.totalLen - PRELUDE_BYTE_LEN_WITH_CRC - MESSAGE_CRC_BYTE_LEN
            check(messageBuffer.request(remaining.toLong())) { "Invalid buffer, not enough remaining; have: ${messageBuffer.size}; expected $remaining" }

            val message = MessageBuilder()

            // read headers
            var headerBytesConsumed = 0L
            while (headerBytesConsumed < prelude.headersLength.toLong()) {
                val start = messageBuffer.buffer.size
                val header = Header.decode(messageBuffer)
                headerBytesConsumed += start - messageBuffer.buffer.size
                message.addHeader(header)
            }
            check(headerBytesConsumed == prelude.headersLength.toLong()) { "Invalid Message: expected ${prelude.headersLength} header bytes; consumed $headerBytesConsumed" }

            message.payload = messageBuffer.readByteArray(prelude.payloadLen.toLong())

            val expectedCrc = source.readInt().toUInt()
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
    public fun encode(dest: SdkBufferedSink) {
        val headerBuf = SdkBuffer()
        headers.forEach { it.encode(headerBuf) }
        val headersLen = headerBuf.size
        val payloadLen = payload.size

        val messageLen = PRELUDE_BYTE_LEN_WITH_CRC + headersLen + payloadLen + MESSAGE_CRC_BYTE_LEN
        check(headersLen < MAX_HEADER_SIZE) { "Invalid Headers length: $headersLen" }
        check(messageLen < MAX_MESSAGE_SIZE) { "Invalid Message length: $messageLen" }

        val prelude = Prelude(messageLen.toInt(), headersLen.toInt())

        val sink = CrcSink(dest)
        val buffer = sink.buffer()

        prelude.encode(buffer)
        buffer.write(headerBuf, headerBuf.size)
        buffer.write(payload)

        buffer.emit()
        dest.writeInt(sink.crc.toInt())
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
