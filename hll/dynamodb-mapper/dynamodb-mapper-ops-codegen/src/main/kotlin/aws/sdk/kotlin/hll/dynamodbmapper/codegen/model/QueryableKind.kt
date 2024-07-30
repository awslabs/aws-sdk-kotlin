/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

/**
 * Identifies a type in the `Queryable<T>` hierarchy
 */
enum class QueryableKind(val parent: QueryableKind? = null) {
    /**
     * Indicates the `Queryable<T>` interface
     */
    Queryable,

    /**
     * Indicates the `Index<T>` interface
     */
    Index(Queryable),

    /**
     * Indicates the `Table<T>` interface
     */
    Table(Queryable),
}

/**
 * Identifies the types of `Queryable` on which an operation can be invoked (e.g., `Scan` can be invoked on a table,
 * index, or queryable, whereas `GetItem` can only be invoked on a table)
 */
val Operation.queryableKinds: Set<QueryableKind>
    get() = when (name) {
        "Query", "Scan" -> setOf(QueryableKind.Queryable, QueryableKind.Index, QueryableKind.Table)
        else -> setOf(QueryableKind.Table)
    }
