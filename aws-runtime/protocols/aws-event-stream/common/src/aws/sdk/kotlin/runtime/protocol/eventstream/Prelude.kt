/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.*

internal const val PRELUDE_BYTE_LEN = 8
internal const val PRELUDE_BYTE_LEN_WITH_CRC = PRELUDE_BYTE_LEN + 4

/**
 * A single even stream message prelude
 * @param totalLen The total byte length of the message including the prelude and message CRC
 * @param headersLength The byte length of all headers
 */
@InternalSdkApi
public data class Prelude(val totalLen: Int, val headersLength: Int) {
    /**
     * The byte length of the message payload
     */
    val payloadLen: Int
        get() = totalLen - PRELUDE_BYTE_LEN_WITH_CRC - headersLength - MESSAGE_CRC_BYTE_LEN

    /**
     * Encode the prelude + CRC to [dest] buffer
     */
    public fun encode(dest: SdkBufferedSink) {
        val sink = CrcSink(dest)
        val buffer = sink.buffer()

        buffer.writeInt(totalLen)
        buffer.writeInt(headersLength)
        buffer.emit()
        dest.writeInt(sink.crc.toInt())
    }

    public companion object {
        /**
         * Read the prelude from [source] and validate the prelude CRC
         */
        public fun decode(source: SdkBufferedSource): Prelude {
            check(source.request(PRELUDE_BYTE_LEN_WITH_CRC.toLong())) { "Invalid message prelude" }
            val crcSource = CrcSource(source)
            val buffer = SdkBuffer()
            crcSource.read(buffer, PRELUDE_BYTE_LEN.toLong())
            val expectedCrc = source.readInt().toUInt()
            val computedCrc = crcSource.crc

            val totalLen = buffer.readInt()
            val headerLen = buffer.readInt()

            check(totalLen <= MAX_MESSAGE_SIZE) { "Invalid Message size: $totalLen" }
            check(headerLen <= MAX_HEADER_SIZE) { "Invalid Header size: $headerLen" }
            check(expectedCrc == computedCrc) {
                "Prelude checksum mismatch; expected=0x${expectedCrc.toString(16)}; calculated=0x${computedCrc.toString(16)}"
            }
            return Prelude(totalLen, headerLen)
        }
    }
}
