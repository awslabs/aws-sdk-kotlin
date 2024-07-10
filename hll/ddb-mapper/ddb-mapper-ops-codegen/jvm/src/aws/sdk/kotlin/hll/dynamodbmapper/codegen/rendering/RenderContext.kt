/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.core.CodeGeneratorFactory
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

internal data class RenderContext(val logger: KSPLogger, val codegenFactory: CodeGeneratorFactory, val pkg: String)

internal fun RenderContext.logging(message: String, symbol: KSNode? = null) = logger.logging(message, symbol)
internal fun RenderContext.info(message: String, symbol: KSNode? = null) = logger.info(message, symbol)
internal fun RenderContext.warn(message: String, symbol: KSNode? = null) = logger.warn(message, symbol)
internal fun RenderContext.error(message: String, symbol: KSNode? = null) = logger.error(message, symbol)

internal fun RenderContext.exception(e: Throwable) = logger.exception(e)
