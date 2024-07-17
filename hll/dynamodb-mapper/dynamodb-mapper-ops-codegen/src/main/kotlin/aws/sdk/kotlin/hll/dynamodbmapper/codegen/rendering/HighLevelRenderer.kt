/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param ctx The active [RenderContext]
 * @param operations A list of the operations in scope for codegen
 */
class HighLevelRenderer(private val ctx: RenderContext, private val operations: List<Operation>) {
    fun render() {
        operations.forEach(::render)
        TableOperationsRenderer(ctx, operations).render()
    }

    private fun render(operation: Operation) {
        OperationRenderer(ctx, operation).render()
    }
}
