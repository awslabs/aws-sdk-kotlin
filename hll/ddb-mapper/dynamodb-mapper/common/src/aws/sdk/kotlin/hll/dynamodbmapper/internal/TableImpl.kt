/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.Table
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
            item = schema.converter.toItem(obj)
        }
    }

    override fun scan(): Flow<T> {
        val resp = mapper.client.scanPaginated {
            tableName = name
        }
        return resp.items().map { schema.converter.fromItem(it.toItem()) }
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
