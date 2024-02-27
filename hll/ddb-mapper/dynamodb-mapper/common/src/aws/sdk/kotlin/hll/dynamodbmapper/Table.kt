/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.itemOf
import aws.sdk.kotlin.hll.dynamodbmapper.schemas.ItemSchema
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.getItem
import aws.sdk.kotlin.services.dynamodb.paginators.items
import aws.sdk.kotlin.services.dynamodb.paginators.scanPaginated
import aws.sdk.kotlin.services.dynamodb.putItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// TODO refactor to interface, add support for all operations, document, add unit tests
public sealed class Table<I>(public val client: DynamoDbClient, public val name: String) {
    public abstract val schema: ItemSchema<I>

    public companion object {
        public operator fun <I, PK> invoke(
            client: DynamoDbClient,
            name: String,
            schema: ItemSchema.PartitionKey<I, PK>,
        ): PartitionKey<I, PK> = PartitionKey(client, name, schema)

        public operator fun <I, PK, SK> invoke(
            client: DynamoDbClient,
            name: String,
            schema: ItemSchema.CompositeKey<I, PK, SK>,
        ): CompositeKey<I, PK, SK> = CompositeKey(client, name, schema)
    }

    public fun scan(): Flow<I> {
        val resp = client.scanPaginated {
            tableName = name
        }
        return resp.items().map { schema.converter.fromItem(Item(it)) }
    }

    internal suspend fun getItem(key: Item): I? {
        val resp = client.getItem {
            tableName = name
            this.key = key
        }
        return resp.item?.let { schema.converter.fromItem(Item(it)) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("getItemByKeyItem")
    public abstract suspend fun getItem(keyItem: I): I?

    public suspend fun putItem(item: I) {
        client.putItem {
            tableName = name
            this.item = schema.converter.toItem(item)
        }
    }

    public class PartitionKey<I, PK> internal constructor(
        client: DynamoDbClient,
        name: String,
        override val schema: ItemSchema.PartitionKey<I, PK>,
    ) : Table<I>(client, name) {
        private val keyAttributeNames = setOf(schema.partitionKey.name)

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("getItemByKeyItem")
        override suspend fun getItem(keyItem: I): I? =
            getItem(schema.converter.toItem(keyItem, keyAttributeNames))

        public suspend fun getItem(partitionKey: PK): I? =
            getItem(itemOf(schema.partitionKey.toField(partitionKey)))
    }

    public class CompositeKey<I, PK, SK> internal constructor(
        client: DynamoDbClient,
        name: String,
        override val schema: ItemSchema.CompositeKey<I, PK, SK>,
    ) : Table<I>(client, name) {
        private val keyAttributeNames = setOf(schema.partitionKey.name, schema.sortKey.name)

        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("getItemByKeyItem")
        override suspend fun getItem(keyItem: I): I? =
            getItem(schema.converter.toItem(keyItem, keyAttributeNames))

        public suspend fun getItem(partitionKey: PK, sortKey: SK): I? =
            getItem(itemOf(schema.partitionKey.toField(partitionKey), schema.sortKey.toField(sortKey)))
    }
}
