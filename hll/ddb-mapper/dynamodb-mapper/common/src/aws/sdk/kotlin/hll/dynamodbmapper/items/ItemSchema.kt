/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

// TODO maybe add a default factory method to ease creation by users outside of codegen?

/**
 * Defines a schema for handling objects of a certain type, including an [ItemConverter] for converting between objects
 * items and a [KeySpec] for identifying primary keys.
 * @param T The type of objects described by this schema
 */
public interface ItemSchema<T> {
    /**
     * The [ItemConverter] used to convert between objects and items
     */
    public val converter: ItemConverter<T>

    /**
     * Represents a schema with a primary key consisting of a single partition key
     * @param T The type of objects described by this schema
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, in PK> : ItemSchema<T> {
        /**
         * The [KeySpec] for the partition key
         */
        public val partitionKey: KeySpec<PK>
    }

    /**
     * Represents a schema with a primary key that is a composite of a partition key and a sort key
     * @param T The type of objects described by this schema
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> : PartitionKey<T, PK> {
        /**
         * The [KeySpec] for the sort key
         */
        public val sortKey: KeySpec<SK>
    }
}

/**
 * Associate this [ItemConverter] with a [KeySpec] for a partition key to form a complete [ItemSchema]
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
 * @param partitionKey The [KeySpec] that describes the partition key
 */
public fun <T, PK> ItemConverter<T>.withKeySpec(partitionKey: KeySpec<PK>): ItemSchema.PartitionKey<T, PK> =
    object : ItemSchema.PartitionKey<T, PK> {
        override val converter = this@withKeySpec
        override val partitionKey = partitionKey
    }

/**
 * Associate this [ItemConverter] with [KeySpec] instances for a composite key to form a complete [ItemSchema]
 * @param T The type of objects described by this schema
 * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
 * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
 * @param partitionKey The [KeySpec] that describes the partition key
 * @param sortKey The [KeySpec] that describes the sort key
 */
public fun <T, PK, SK> ItemConverter<T>.withKeySpec(
    partitionKey: KeySpec<PK>,
    sortKey: KeySpec<SK>,
): ItemSchema.CompositeKey<T, PK, SK> =
    object : ItemSchema.CompositeKey<T, PK, SK> {
        override val converter = this@withKeySpec
        override val partitionKey = partitionKey
        override val sortKey = sortKey
    }
