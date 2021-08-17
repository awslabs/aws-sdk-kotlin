/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.testing

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

/**
 * Extension of File that creates a temporary file with a specified name in
 * Java's temporary directory, as declared in the JRE's system properties. The
 * file is immediately filled with a specified amount of random ASCII data.
 *
 * @see RandomInputStream
 */
public class RandomTempFile : File {
    /** Flag controlling whether binary or character data is used.  */
    private val binaryData: Boolean

    /**
     * Creates, and fills, a temp file with a randomly generated name and specified size of random ASCII data.
     *
     * @param sizeInBytes The amount of random ASCII data, in bytes, for the new temp
     * file.
     * @throws IOException If any problems were encountered creating the new temp file.
     */
    public constructor(sizeInBytes: Long) : this(UUID.randomUUID().toString(), sizeInBytes, false)

    /**
     * Creates, and fills, a temp file with the specified name and specified
     * size of random data.
     *
     * @param filename The name for the new temporary file, within the Java temp
     * directory as declared in the JRE's system properties.
     * @param sizeInBytes The amount of random ASCII data, in bytes, for the new temp
     * file.
     * @param binaryData Whether to fill the file with binary or character data.
     *
     * @throws IOException
     * If any problems were encountered creating the new temp file.
     */
    public constructor(filename: String, sizeInBytes: Long, binaryData: Boolean = false) : super(
        TEMP_DIR + separator + System.currentTimeMillis().toString() + "-" + filename
    ) {
        this.binaryData = binaryData
        createFile(sizeInBytes)
    }

    @Throws(IOException::class)
    public fun createFile(sizeInBytes: Long) {
        deleteOnExit()
        FileOutputStream(this).use { outputStream ->
            BufferedOutputStream(outputStream).use { bufferedOutputStream ->
                RandomInputStream(sizeInBytes, binaryData).use { inputStream ->
                    inputStream.copyTo(bufferedOutputStream)
                }
            }
        }
    }

    override fun delete(): Boolean {
        if (!super.delete()) {
            throw RuntimeException("Could not delete: $absolutePath")
        }
        return true
    }

    public companion object {
        private val TEMP_DIR: String = System.getProperty("java.io.tmpdir")
    }
}
