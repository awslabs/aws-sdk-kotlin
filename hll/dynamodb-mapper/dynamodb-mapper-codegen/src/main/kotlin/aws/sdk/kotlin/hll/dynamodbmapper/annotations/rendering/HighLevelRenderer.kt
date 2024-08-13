/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param annotatedClasses A list of annotated classes
 */
public class HighLevelRenderer(
    private val annotatedClasses: List<KSClassDeclaration>,
    private val logger: KSPLogger,
    private val codegenFactory: CodeGeneratorFactory,
) {
    public fun render() {
        annotatedClasses.forEach {
            logger.info("Processing annotation on ${it.simpleName}")
            val renderCtx = RenderContext(logger, codegenFactory, "${it.packageName.asString()}.mapper.schemas")
            val annotation = SchemaRenderer(it, renderCtx)
            annotation.render()
        }
    }
}
