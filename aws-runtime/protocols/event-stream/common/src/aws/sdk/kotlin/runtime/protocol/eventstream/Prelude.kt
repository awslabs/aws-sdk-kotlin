/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.*
import aws.smithy.kotlin.runtime.util.crc32

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
    public fun encode(dest: SdkBuffer) {
        val bytes = ByteArray(PRELUDE_BYTE_LEN)
        val preludeBuf = SdkBuffer.of(bytes)

        preludeBuf.writeInt(totalLen)
        preludeBuf.writeInt(headersLength)

        dest.writeFully(preludeBuf)
        dest.writeInt(bytes.crc32().toInt())
    }

    public companion object {
        /**
         * Read the prelude from [buffer] and validate the prelude CRC
         */
        public fun decode(buffer: SdkBuffer): Prelude {
            check(buffer.readRemaining >= PRELUDE_BYTE_LEN_WITH_CRC) { "Invalid message prelude" }
            val crcBuffer = ByteArray(PRELUDE_BYTE_LEN)
            buffer.readFully(crcBuffer)
            val computedCrc = crcBuffer.crc32()
            buffer.rewind(PRELUDE_BYTE_LEN)

            val totalLen = buffer.readInt()
            val headerLen = buffer.readInt()
            val expectedCrc = buffer.readUInt()

            check(headerLen <= MAX_HEADER_SIZE) { "Invalid Header size: $headerLen" }
            check(expectedCrc == computedCrc) {
                "Prelude checksum mismatch; expected=0x${expectedCrc.toString(16)}; calculated=0x${computedCrc.toString(16)}"
            }
            return Prelude(totalLen, headerLen)
        }
    }
}
