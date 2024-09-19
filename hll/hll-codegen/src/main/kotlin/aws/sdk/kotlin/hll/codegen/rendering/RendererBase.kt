/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * The parent class for renderers backed by a [CodeGenerator]
 * @param ctx The active [RenderContext]
 * @param fileName The name of the file which should be created _without_ parent directory or extension (which is always
 * **.kt**)
 */
@InternalSdkApi
public abstract class RendererBase(
    ctx: RenderContext,
    fileName: String,
) : CodeGenerator by ctx.codegenFactory.generator(fileName, ctx.pkg, ctx.rendererName) {
    /**
     * Run this renderer by calling the `abstract` [generate] method and then [persist]
     */
    public fun render() {
        generate()
        persist()
    }

    protected abstract fun generate()
}
