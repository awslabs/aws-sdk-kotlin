/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.QueryableKind
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.queryableKinds

/**
 * The parent renderer for all codegen from this package. This class orchestrates the various sub-renderers.
 * @param ctx The active [RenderContext]
 * @param operations A list of the operations in scope for codegen
 */
class HighLevelRenderer(private val ctx: RenderContext, private val operations: List<Operation>) {
    fun render() {
        operations.forEach(::render)

        val operationsRenderers = mutableMapOf<QueryableKind, QueryableOperationsRenderer>()
        QueryableKind.entries.forEach { qk ->
            ctx.warn("About to generate hll operations for $qk")
            val parentType = qk.parent?.let { operationsRenderers[it] }?.parentType
            val operations = this.operations.filter { qk in it.queryableKinds }

            val renderer = QueryableOperationsRenderer(ctx, qk, parentType, operations)
            renderer.render()
            operationsRenderers += qk to renderer
        }
    }

    private fun render(operation: Operation) {
        OperationRenderer(ctx, operation).render()
    }
}
