/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.ksp.processors

import aws.sdk.kotlin.hll.codegen.util.KspLoggerOutputStream
import aws.sdk.kotlin.hll.codegen.util.asPrintStream
import aws.sdk.kotlin.runtime.InternalSdkApi
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.PrintStream

/**
 * An abstract implementation for all KSP symbol processors used in high-level libraries
 */
@InternalSdkApi
public abstract class HllKspProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var invoked = false
    private val logger = environment.logger
    private var originalStdOut: PrintStream? = null

    final override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            logger.info("${this::class.simpleName} has already run once; skipping subsequent processing rounds")
            return listOf()
        } else {
            invoked = true
        }

        if (originalStdOut == null) {
            val loggerOutStream = KspLoggerOutputStream(logger)
            System.setOut(loggerOutStream.asPrintStream())
        }

        return processImpl(resolver)
    }

    protected abstract fun processImpl(resolver: Resolver): List<KSAnnotated>

    final override fun finish() {
        originalStdOut?.let(System::setOut)
    }
}
