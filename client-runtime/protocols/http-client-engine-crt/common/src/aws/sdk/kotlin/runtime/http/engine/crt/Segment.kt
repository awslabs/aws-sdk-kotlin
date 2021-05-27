/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.http.engine.crt

import software.aws.clientrt.io.SdkBuffer
import software.aws.clientrt.io.writeFully

// FIXME - let's just get rid of this and ReadableBuffer in favor of making SdkBuffer
// or SdkBuffer / MutableSdkBuffer -> SdkBufferImpl
// this would allow SdkBuffer.of(byteArray)

/**
 * Convenience wrapper around a byte array instance that stores current read state
 */
internal data class Segment(
    private val buffer: ByteArray,
    private var readHead: Int = 0,
) {
    val availableForRead: Int
        get() = buffer.size - readHead

    /**
     * Attempt to copy up to [length] bytes into [dest] starting at [offset]
     * Returns the number of bytes actually copied which may be less than requested
     */
    fun copyTo(dest: ByteArray, offset: Int = 0, length: Int = dest.size - offset): Int {
        check(availableForRead > 0) { "nothing left to read from segment" }

        val rc = minOf(length, availableForRead)

        val endIdxExclusive = readHead + rc
        buffer.copyInto(dest, offset, readHead, endIdxExclusive)
        readHead += rc
        return rc
    }

    fun copyTo(dest: SdkBuffer, limit: Int = Int.MAX_VALUE): Int {
        check(availableForRead > 0) { "nothing left to read from segment" }
        val wc = minOf(buffer.size - readHead, limit)
        dest.writeFully(buffer, offset = readHead, length = wc)
        readHead += wc
        return wc
    }
}
