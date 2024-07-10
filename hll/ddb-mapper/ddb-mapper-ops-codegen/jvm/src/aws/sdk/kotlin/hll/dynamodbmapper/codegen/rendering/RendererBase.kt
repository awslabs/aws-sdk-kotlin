/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.core.CodeGenerator

internal abstract class RendererBase(
    ctx: RenderContext,
    name: String,
) : CodeGenerator by ctx.codegenFactory.generator(name, ctx.pkg) {
    fun render() {
        generate()
        persist()
    }

    protected abstract fun generate()
}
