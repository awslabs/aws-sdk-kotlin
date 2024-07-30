/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.Index
import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec
import aws.sdk.kotlin.hll.dynamodbmapper.model.Table
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperations
import aws.sdk.kotlin.hll.dynamodbmapper.operations.TableOperationsImpl

internal fun <T, PK> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.PartitionKey<T, PK>,
): Table.PartitionKey<T, PK> {
    val tableName = name // shadowed below
    val specImpl = PersistenceSpecPartitionKeyImpl(mapper, tableName, indexName = null, schema)
    val opsImpl = TableOperationsImpl(specImpl)
    return object :
        Table.PartitionKey<T, PK>,
        PersistenceSpec.PartitionKey<T, PK> by specImpl,
        TableOperations<T> by opsImpl {

        override fun <T, PK> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK, SK> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)

        override suspend fun getItem(partitionKey: PK) = TODO("not yet implemented")
    }
}

internal fun <T, PK, SK> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): Table.CompositeKey<T, PK, SK> {
    val specImpl = PersistenceSpecCompositeKeyImpl(mapper, name, indexName = null, schema)
    val opsImpl = TableOperationsImpl(specImpl)
    return object :
        Table.CompositeKey<T, PK, SK>,
        PersistenceSpec.CompositeKey<T, PK, SK> by specImpl,
        TableOperations<T> by opsImpl {

        override fun <T, PK> getIndex(
            name: String,
            schema: ItemSchema.PartitionKey<T, PK>,
        ): Index.PartitionKey<T, PK> = indexImpl(mapper, tableName, name, schema)

        override fun <T, PK, SK> getIndex(
            name: String,
            schema: ItemSchema.CompositeKey<T, PK, SK>,
        ): Index.CompositeKey<T, PK, SK> = indexImpl(mapper, tableName, name, schema)

        override suspend fun getItem(partitionKey: PK, sortKey: SK) = TODO("Not yet implemented")
    }
}
