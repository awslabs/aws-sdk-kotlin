/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.processor.rendering

import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param annotations A list of annotated classes
 */
public class HighLevelRenderer(
    private val ctx: RenderContext,
    private val annotations: List<KSClassDeclaration>
) {
    public fun render() {
        annotations.forEach {
            val renderCtx = RenderContext(ctx.logger, ctx.codegenFactory, "${it.packageName.asString()}.mapper.schemas")
            val annotation = AnnotationRenderer(it, renderCtx)
            annotation.render()
        }
    }
}
