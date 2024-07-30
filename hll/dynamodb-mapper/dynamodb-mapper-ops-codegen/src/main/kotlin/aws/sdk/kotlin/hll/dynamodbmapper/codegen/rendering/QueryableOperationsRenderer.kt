/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.*
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.lowercaseFirstChar

/**
 * Renders the `*Operations` interface and `*OperationsImpl` class which contain a method for each codegenned
 * operation and dispatches to the factory function rendered in [OperationRenderer]
 * @param ctx The active [RenderContext]
 * @param queryableKind The type of queryable for which to render operations
 * @param parentType The [Type] of the direct parent interface of the to-be-generated `*Operations` interface (e.g., if
 * [queryableKind] is [QueryableKind.Table], then [parentType] should be the generated `QueryableOperations` interface)
 * @param operations A list of the operations in scope for codegen
 */
class QueryableOperationsRenderer(
    private val ctx: RenderContext,
    val queryableKind: QueryableKind,
    val parentType: Type?,
    val operations: List<Operation>,
) : RendererBase(ctx, "${queryableKind.name}Operations") {
    private val entityName = queryableKind.name.lowercaseFirstChar
    private val intfName = "${queryableKind.name}Operations"
    private val intfType = TypeRef(ctx.pkg, intfName, listOf(TypeVar("T")))

    override fun generate() {
        renderInterface()
        blankLine()
        renderImpl()
        // TODO also render DSL extension methods (e.g., table.getItem { key = ... })
    }

    private fun renderImpl() {
        val implName = "${queryableKind.name}OperationsImpl"

        withBlock(
            "internal class #L<T>(private val spec: #T) : #T {",
            "}",
            implName,
            Types.persistenceSpec("T"),
            intfType,
        ) {
            operations.forEach { operation ->
                write(
                    "override suspend fun #L(request: #T) = #L(spec).execute(request)",
                    operation.methodName,
                    operation.request.type,
                    OperationRenderer.factoryFunctionName(operation),
                )
            }
        }
    }

    private fun renderInterface() {
        withDocs {
            write("Provides access to operations on a particular #L, which will invoke low-level", entityName)
            write("operations after mapping objects to items and vice versa")
            write("@param T The type of objects which will be read from and/or written to this #L", entityName)
        }

        writeInline("public interface #T ", intfType)

        parentType?.let { writeInline(": #T ", parentType) }

        withBlock("{", "}") {
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
