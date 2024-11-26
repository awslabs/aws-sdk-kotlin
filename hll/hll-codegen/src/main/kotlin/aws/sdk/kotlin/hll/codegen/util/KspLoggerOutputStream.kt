/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

import aws.sdk.kotlin.runtime.InternalSdkApi
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream

private const val NEWLINE = 0xa // \n

/**
 * Wraps a [KSPLogger] as an [OutputStream], enabling JVM-standard stream operations on the logger. All data written to
 * this stream will be buffered until a newline is encountered. Once a newline is encountered, the buffered data is
 * emitted to the logger (_without_ the newline) and the buffer is cleared.
 * @param logger The logger instance to which data should be written
 * @param logMethod The specific logger method to use on the logger. The default value is [KSPLogger.info].
 */
@InternalSdkApi
public class KspLoggerOutputStream(
    private val logger: KSPLogger,
    private val logMethod: KSPLogger.(message: String, symbol: KSNode?) -> Unit = KSPLogger::info,
) : OutputStream() {
    private val currentLine = ByteArrayOutputStream()

    override fun write(b: Int) {
        if (b == NEWLINE) flush() else currentLine.write(b)
    }

    override fun flush() {
        logger.info(currentLine.toByteArray().decodeToString())
        currentLine.reset()
    }
}

/**
 * Wraps a [KspLoggerOutputStream] in a [PrintStream]
 */
@InternalSdkApi
public fun KspLoggerOutputStream.asPrintStream(): PrintStream = PrintStream(this)
