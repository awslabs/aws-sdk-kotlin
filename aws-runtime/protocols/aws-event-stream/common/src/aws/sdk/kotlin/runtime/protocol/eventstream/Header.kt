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
         * Read an encoded header from the [source]
         */
        public fun decode(source: SdkBufferedSource): Header {
            check(source.request(MIN_HEADER_LEN.toLong())) { "Invalid frame header; require at least $MIN_HEADER_LEN bytes" }
            val nameLen = source.readByte().toInt()
            check(nameLen > 0) { "Invalid header name length: $nameLen" }
            check(source.request(nameLen.toLong())) { "Not enough bytes to read header name; needed: $nameLen; remaining: ${source.buffer.size}" }
            val name = source.readUtf8(nameLen.toLong())
            val value = HeaderValue.decode(source)
            return Header(name, value)
        }
    }

    /**
     * Encode a header to [dest] buffer
     */
    public fun encode(dest: SdkBufferedSink) {
        val bytes = name.encodeToByteArray()
        check(bytes.size < MAX_HEADER_NAME_LEN) { "Header name too long" }
        dest.writeByte(bytes.size.toByte())
        dest.write(bytes)
        value.encode(dest)
    }
}
