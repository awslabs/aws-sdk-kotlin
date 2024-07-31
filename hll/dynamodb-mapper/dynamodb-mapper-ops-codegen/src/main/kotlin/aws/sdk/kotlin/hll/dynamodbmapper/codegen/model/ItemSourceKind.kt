/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

/**
 * Identifies a type in the `ItemSource<T>` hierarchy
 */
enum class ItemSourceKind(val parent: ItemSourceKind? = null) {
    /**
     * Indicates the `ItemSource<T>` interface
     */
    ItemSource,

    /**
     * Indicates the `Index<T>` interface
     */
    Index(ItemSource),

    /**
     * Indicates the `Table<T>` interface
     */
    Table(ItemSource),
}

/**
 * Identifies the types of `ItemSource` on which an operation can be invoked (e.g., `Scan` can be invoked on a table,
 * index, or any generic item source, whereas `GetItem` can only be invoked on a table)
 */
val Operation.itemSourceKinds: Set<ItemSourceKind>
    get() = when (name) {
        "Query", "Scan" -> setOf(ItemSourceKind.ItemSource, ItemSourceKind.Index, ItemSourceKind.Table)
        else -> setOf(ItemSourceKind.Table)
    }
