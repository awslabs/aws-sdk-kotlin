/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.getItem
import aws.sdk.kotlin.services.dynamodb.paginators.items
import aws.sdk.kotlin.services.dynamodb.paginators.scanPaginated
import aws.sdk.kotlin.services.dynamodb.putItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Represents a table in DynamoDB and an associated item schema. Operations on this table will invoke low-level
 * operations after mapping objects to items and vice versa.
 * @param T The type of objects which will be read from and/or written to this table
 */
public interface Table<T> {
    /**
     * The [DynamoDbMapper] which holds the underlying DynamoDB service client used to invoke operations
     */
    public val mapper: DynamoDbMapper

    /**
     * The name of this table
     */
    public val name: String

    /**
     * The [ItemSchema] for this table which describes how to map objects to items and vice versa
     */
    public val schema: ItemSchema<T>

    // TODO reimplement operations to use pipeline, extension functions where appropriate, docs, etc.

    public suspend fun getItem(key: Item): T?

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getItemByKeyObj")
    public suspend fun getItem(keyObj: T): T?

    public suspend fun putItem(obj: T)

    public fun scan(): Flow<T>

    /**
     * Represents a table whose primary key is a single partition key
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, PK> : Table<T> {
        // TODO reimplement operations to use pipeline, extension functions where appropriate, docs, etc.
        public suspend fun getItem(partitionKey: PK): T?
    }

    /**
     * Represents a table whose primary key is a composite of a partition key and a sort key
     * @param T The type of objects which will be read from and/or written to this table
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> : Table<T> {
        // TODO reimplement operations to use pipeline, extension functions where appropriate, docs, etc.
        public suspend fun getItem(partitionKey: PK, sortKey: SK): T?
    }
}

internal abstract class TableImpl<T>(override val mapper: DynamoDbMapper, override val name: String) : Table<T> {
    override suspend fun getItem(key: Item): T? {
        val resp = mapper.client.getItem {
            tableName = name
            this.key = key
        }
        return resp.item?.toItem()?.let(schema.converter::fromItem)
    }

    override suspend fun putItem(obj: T) {
        mapper.client.putItem {
            tableName = name
            this.item = schema.converter.toItem(obj)
        }
    }

    override fun scan(): Flow<T> {
        val resp = mapper.client.scanPaginated {
            tableName = name
        }
        return resp.items().map { schema.converter.fromItem(it.toItem() ) }
    }

    internal class PartitionKeyImpl<T, PK> internal constructor(
        mapper: DynamoDbMapper,
        name: String,
        override val schema: ItemSchema.PartitionKey<T, PK>,
    ) : TableImpl<T>(mapper, name), Table.PartitionKey<T, PK> {
        private val keyAttributeNames = setOf(schema.partitionKey.name)

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("getItemByKeyItem")
        override suspend fun getItem(keyObj: T): T? =
            getItem(schema.converter.toItem(keyObj, keyAttributeNames))

        override suspend fun getItem(partitionKey: PK): T? =
            getItem(itemOf(schema.partitionKey.toField(partitionKey)))
    }

    internal class CompositeKeyImpl<T, PK, SK> internal constructor(
        mapper: DynamoDbMapper,
        name: String,
        override val schema: ItemSchema.CompositeKey<T, PK, SK>,
    ) : TableImpl<T>(mapper, name), Table.CompositeKey<T, PK, SK> {
        private val keyAttributeNames = setOf(schema.partitionKey.name, schema.sortKey.name)

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("getItemByKeyItem")
        override suspend fun getItem(keyObj: T): T? =
            getItem(schema.converter.toItem(keyObj, keyAttributeNames))

        override suspend fun getItem(partitionKey: PK, sortKey: SK): T? =
            getItem(itemOf(schema.partitionKey.toField(partitionKey), schema.sortKey.toField(sortKey)))
    }
}
