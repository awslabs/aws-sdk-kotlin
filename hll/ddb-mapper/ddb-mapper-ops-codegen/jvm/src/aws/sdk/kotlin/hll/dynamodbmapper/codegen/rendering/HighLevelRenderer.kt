/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation

internal class HighLevelRenderer(private val ctx: RenderContext, private val operations: List<Operation>) {
    fun render() {
        operations.forEach(::render)
        TableOperationsRenderer(ctx, operations).render()
    }

    private fun render(operation: Operation) {
        OperationRenderer(ctx, operation).render()
    }
}
