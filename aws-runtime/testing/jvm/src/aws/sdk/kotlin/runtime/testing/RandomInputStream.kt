/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.testing

import java.io.IOException
import java.io.InputStream
import kotlin.random.Random

/**
 * Test utility InputStream implementation that generates random ASCII data when
 * read, up to the size specified when constructed.
 * NOTE: Adapted from
 */
public class RandomInputStream @JvmOverloads constructor(
    /** The requested amount of data contained in this random stream.  */
    protected val lengthInBytes: Long,

    /** Flag controlling whether binary or character data is used.  */
    private val binaryData: Boolean = false
) : InputStream() {

    /** The number of bytes of data remaining in this random stream.  */
    protected var remainingBytes: Long = lengthInBytes

    public val bytesRead: Long
        get() = lengthInBytes - remainingBytes

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        // Signal that we're out of data if we've hit our limit
        if (remainingBytes <= 0) {
            return -1
        }
        var bytesToRead = len
        if (bytesToRead > remainingBytes) {
            bytesToRead = remainingBytes.toInt()
        }
        remainingBytes -= bytesToRead.toLong()
        if (binaryData) {
            val bytes = ByteArray(bytesToRead)
            Random.nextBytes(bytes)
            System.arraycopy(bytes, 0, b, off, bytesToRead)
        } else {
            for (i in 0 until bytesToRead) {
                b[off + i] = Random.nextInt(MIN_CHAR_CODE, MAX_CHAR_CODE + 1).toByte()
            }
        }
        return bytesToRead
    }

    @Throws(IOException::class)
    override fun read(): Int {
        // Signal that we're out of data if we've hit our limit
        if (remainingBytes <= 0) {
            return -1
        }
        remainingBytes--
        return if (binaryData) {
            val bytes = ByteArray(1)
            Random.nextBytes(bytes)
            bytes[0].toInt()
        } else {
            Random.nextInt(MIN_CHAR_CODE, MAX_CHAR_CODE + 1)
        }
    }

    public companion object {
        /** The minimum ASCII code contained in the data in this stream.  */
        private const val MIN_CHAR_CODE = 32

        /** The maximum ASCII code contained in the data in this stream.  */
        private const val MAX_CHAR_CODE = 125
    }
}
