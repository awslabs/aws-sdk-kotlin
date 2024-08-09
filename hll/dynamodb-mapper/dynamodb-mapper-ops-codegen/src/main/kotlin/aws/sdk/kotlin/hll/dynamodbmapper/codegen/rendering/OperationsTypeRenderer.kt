/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.*
import aws.sdk.kotlin.hll.codegen.util.lowercaseFirstChar

/**
 * Renders the `*Operations` interface and `*OperationsImpl` class which contain a method for each codegenned
 * operation and dispatches to the factory function rendered in [OperationRenderer]
 * @param ctx The active [RenderContext]
 * @param itemSourceKind The type of `ItemSource` for which to render operations
 * @param parentType The [Type] of the direct parent interface of the to-be-generated `*Operations` interface (e.g., if
 * [itemSourceKind] is [ItemSourceKind.Table], then [parentType] should be the generated `ItemSourceOperations`
 * interface)
 * @param operations A list of the operations in scope for codegen
 */
class OperationsTypeRenderer(
    private val ctx: RenderContext,
    val itemSourceKind: ItemSourceKind,
    val parentType: Type?,
    val operations: List<Operation>,
) : RendererBase(ctx, "${itemSourceKind.name}Operations") {
    private val entityName = itemSourceKind.name.lowercaseFirstChar
    private val intfName = "${itemSourceKind.name}Operations"

    val interfaceType = TypeRef(ctx.pkg, intfName, listOf(TypeVar("T")))

    override fun generate() {
        renderInterface()

        if (!itemSourceKind.isAbstract) {
            blankLine()
            renderImpl()
        }

        // TODO also render DSL extension methods (e.g., table.getItem { key = ... })
    }

    private fun renderImpl() {
        val implName = "${itemSourceKind.name}OperationsImpl"

        withBlock(
            "internal class #L<T>(private val spec: #T) : #T {",
            "}",
            implName,
            itemSourceKind.getSpecType("T"),
            interfaceType,
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

        writeInline("public interface #T ", interfaceType)

        parentType?.let { writeInline(": #T ", parentType) }

        withBlock("{", "}") {
            operations.forEach { operation ->
                val overrideModifier = if (operation.appliesToAncestorKind()) " override" else ""
                write(
                    "public#L suspend fun #L(request: #T): #T",
                    overrideModifier,
                    operation.methodName,
                    operation.request.type,
                    operation.response.type,
                )
            }
        }
    }

    private fun Operation.appliesToAncestorKind() = itemSourceKind.parent?.let { appliesToKindOrAncestor(it) } ?: false
}

private fun Operation.appliesToKindOrAncestor(kind: ItemSourceKind): Boolean =
    kind in itemSourceKinds || (kind.parent?.let { appliesToKindOrAncestor(it) } ?: false)
