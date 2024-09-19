/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperPkg

/**
 * Identifies a type in the `ItemSource<T>` hierarchy
 * @param hoistedFields Which fields should be hoisted from the low-level request type for this item source kind (e.g.,
 * for `TableSpec<T>` the `tableName` field should be hoisted)
 * @param parent The parent type of this type (if any)
 * @param isAbstract Indicates whether this item source kind is purely abstract and should not have an implementation
 * class (e.g., `ItemSource<T>` should be abstract and non-instantiable)
 */
internal enum class ItemSourceKind(
    val hoistedFields: List<String>,
    val parent: ItemSourceKind? = null,
    val isAbstract: Boolean = false,
) {
    /**
     * Indicates the `ItemSource<T>` interface
     */
    ItemSource(listOf(), isAbstract = true),

    /**
     * Indicates the `Index<T>` interface
     */
    Index(listOf("indexName", "tableName"), ItemSource),

    /**
     * Indicates the `Table<T>` interface
     */
    Table(listOf("tableName"), ItemSource),

    ;

    /**
     * Get the [TypeRef] for the `*Spec` type for this item source kind
     * @param typeVar The type variable name to use for the generic type
     */
    fun getSpecType(typeVar: String): TypeRef = TypeRef(MapperPkg.Hl.Model, "${name}Spec", listOf(TypeVar(typeVar)))
}

/**
 * Identifies the types of `ItemSource` on which an operation can be invoked (e.g., `Scan` can be invoked on a table,
 * index, or any generic item source, whereas `GetItem` can only be invoked on a table)
 */
internal val Operation.itemSourceKinds: Set<ItemSourceKind>
    get() = when (name) {
        "Query", "Scan" -> setOf(ItemSourceKind.ItemSource, ItemSourceKind.Index, ItemSourceKind.Table)
        else -> setOf(ItemSourceKind.Table)
    }
