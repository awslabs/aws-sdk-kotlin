/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Types

internal class TableOperationsRenderer(
    private val ctx: RenderContext,
    val operations: List<Operation>,
) : RendererBase(ctx, "TableOperations") {
    override fun generate() {
        renderInterface()
        blankLine()
        renderImpl()
        // TODO also render DSL extension methods (e.g., table.getItem { key = ... })
    }

    private fun renderImpl() {
        withBlock(
            "internal class TableOperationsImpl<T>(private val tableSpec: #T) : TableOperations<T> {",
            "}",
            Types.tableSpec("T"),
        ) {
            operations.forEach { operation ->
                withBlock("override suspend fun #L(request: #T) =", "", operation.methodName, operation.request.type) {
                    write("#L(tableSpec).execute(request)", OperationRenderer.factoryFunctionName(operation))
                }
            }
        }
    }

    private fun renderInterface() {
        withDocs {
            write("Provides access to operations on a particular table, which will invoke low-level operations after")
            write("mapping objects to items and vice versa")
            write("@param T The type of objects which will be read from and/or written to this table")
        }

        withBlock("public interface TableOperations<T> {", "}") {
            operations.forEach { operation ->
                write(
                    "public suspend fun #L(request: #T): #T",
                    operation.methodName,
                    operation.request.type,
                    operation.response.type,
                )
            }
        }
    }
}
