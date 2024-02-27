/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.schemas

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

// TODO document, add unit tests, maybe add a default factory method to ease creation by users outside of codegen?
public interface ItemSchema<I> {
    public val converter: ItemConverter<I>

    public interface PartitionKey<I, in PK> : ItemSchema<I> {
        public val partitionKey: KeySpec<PK>
    }

    public interface CompositeKey<I, PK, SK> : PartitionKey<I, PK> {
        public val sortKey: KeySpec<SK>
    }
}

public sealed interface KeySpec<in K> {
    public val name: String

    public fun toField(key: K): Pair<String, AttributeValue>

    public data class B(override val name: String) : KeySpec<ByteArray> {
        override fun toField(key: ByteArray): Pair<String, AttributeValue> =
            name to AttributeValue.B(key)
    }

    public data class N(override val name: String) : KeySpec<Number> {
        // FIXME is Number.toString good enough here or do we need more flexibility?
        override fun toField(key: Number): Pair<String, AttributeValue> =
            name to AttributeValue.N(key.toString())
    }

    public data class S(override val name: String) : KeySpec<String> {
        override fun toField(key: String): Pair<String, AttributeValue> =
            name to AttributeValue.S(key)
    }
}

public fun <I, PK> ItemConverter<I>.withKeySpec(partitionKey: KeySpec<PK>): ItemSchema.PartitionKey<I, PK> =
    object : ItemSchema.PartitionKey<I, PK> {
        override val converter = this@withKeySpec
        override val partitionKey = partitionKey
    }

public fun <I, PK, SK> ItemConverter<I>.withKeySpec(
    partitionKey: KeySpec<PK>,
    sortKey: KeySpec<SK>,
): ItemSchema.CompositeKey<I, PK, SK> =
    object : ItemSchema.CompositeKey<I, PK, SK> {
        override val converter = this@withKeySpec
        override val partitionKey = partitionKey
        override val sortKey = sortKey
    }
