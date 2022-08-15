/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.*

private const val MIN_HEADER_LEN = 2
private const val MAX_HEADER_NAME_LEN = 255

/*
    Header Wire Format

    +--------------------+
    |Hdr Name Len (8)    |
    +--------------------+-----------------------------------------------+
    |                            Header Name (*)                     ... |
    +--------------------+-----------------------------------------------+
    |Hdr Value Type (8)  |
    +--------------------+-----------------------------------------------+
    |                           Header Value (*)                     ... |
    +--------------------------------------------------------------------+
*/

/**
 * An event stream frame header
 */
@InternalSdkApi
public data class Header(val name: String, val value: HeaderValue) {
    public companion object {
        /**
         * Read an encoded header from the [buffer]
         */
        public fun decode(buffer: Buffer): Header {
            check(buffer.readRemaining >= MIN_HEADER_LEN.toULong()) { "Invalid frame header; require at least $MIN_HEADER_LEN bytes" }
            val nameLen = buffer.readByte().toInt()
            check(nameLen > 0) { "Invalid header name length: $nameLen" }
            val nameBytes = ByteArray(nameLen)
            buffer.readFully(nameBytes)
            val value = HeaderValue.decode(buffer)
            return Header(nameBytes.decodeToString(), value)
        }
    }

    /**
     * Encode a header to [dest] buffer
     */
    public fun encode(dest: MutableBuffer) {
        val bytes = name.encodeToByteArray()
        check(bytes.size < MAX_HEADER_NAME_LEN) { "Header name too long" }
        dest.writeByte(bytes.size.toByte())
        dest.writeFully(bytes)
        value.encode(dest)
    }
}
