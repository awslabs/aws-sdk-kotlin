/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.hashing.Crc32
import aws.smithy.kotlin.runtime.io.*
import aws.smithy.kotlin.runtime.util.encodeToHex

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
        val sink = HashingSink(Crc32(), dest)
        val buffer = sink.buffer()

        buffer.writeInt(totalLen)
        buffer.writeInt(headersLength)
        buffer.emit()
        dest.write(sink.digest())
    }

    public companion object {
        /**
         * Read the prelude from [source] and validate the prelude CRC
         */
        public fun decode(source: SdkBufferedSource): Prelude {
            check(source.request(PRELUDE_BYTE_LEN_WITH_CRC.toLong())) { "Invalid message prelude" }
            val crcSource = HashingSource(Crc32(), source)
            val buffer = SdkBuffer()
            crcSource.read(buffer, PRELUDE_BYTE_LEN.toLong())

            val expectedCrc = source.readByteArray(4)
            val computedCrc = crcSource.digest()

            val totalLen = buffer.readInt()
            val headerLen = buffer.readInt()

            check(totalLen <= MAX_MESSAGE_SIZE) { "Invalid Message size: $totalLen" }
            check(headerLen <= MAX_HEADER_SIZE) { "Invalid Header size: $headerLen" }
            check(expectedCrc.contentEquals(computedCrc)) {
                "Prelude checksum mismatch; expected=0x${expectedCrc.encodeToHex()}; calculated=0x${computedCrc.encodeToHex()}"
            }
            return Prelude(totalLen, headerLen)
        }
    }
}
