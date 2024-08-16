/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

/**
 * Holds useful context data about code generation. Like many context objects, this is sort of a grab-bag of things
 * which may be useful during various stages.
 * @param logger A logger object for sending events to the console
 * @param codegenFactory A factory that creates code generator instances for specific files
 * @param pkg The Kotlin package for the generated code (e.g., `aws.sdk.kotlin.hll.dynamodbmapper.operations`)
 */
data class RenderContext(val logger: KSPLogger, val codegenFactory: CodeGeneratorFactory, val pkg: String, val rendererName: String = "aws-sdk-kotlin-hll-codegen")

fun RenderContext.logging(message: String, symbol: KSNode? = null) = logger.logging(message, symbol)
fun RenderContext.info(message: String, symbol: KSNode? = null) = logger.info(message, symbol)
fun RenderContext.warn(message: String, symbol: KSNode? = null) = logger.warn(message, symbol)
fun RenderContext.error(message: String, symbol: KSNode? = null) = logger.error(message, symbol)

fun RenderContext.exception(e: Throwable) = logger.exception(e)
