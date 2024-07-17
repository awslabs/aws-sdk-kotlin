/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.Table
import aws.sdk.kotlin.hll.dynamodbmapper.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.*

private data class TableSpecPartitionKeyImpl<T, PK>(
    override val mapper: DynamoDbMapper,
    override val name: String,
    override val schema: ItemSchema.PartitionKey<T, PK>,
) : TableSpec.PartitionKey<T, PK>

private data class TableSpecCompositeKeyImpl<T, PK, SK>(
    override val mapper: DynamoDbMapper,
    override val name: String,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
) : TableSpec.CompositeKey<T, PK, SK>

internal fun <T, PK> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.PartitionKey<T, PK>,
): Table.PartitionKey<T, PK> {
    val specImpl = TableSpecPartitionKeyImpl(mapper, name, schema)
    val opsImpl = TableOperationsImpl(specImpl)
    return object :
        Table.PartitionKey<T, PK>,
        TableSpec.PartitionKey<T, PK> by specImpl,
        TableOperations<T> by opsImpl {
        override suspend fun getItem(partitionKey: PK) = TODO("not yet implemented")
    }
}

internal fun <T, PK, SK> tableImpl(
    mapper: DynamoDbMapper,
    name: String,
    schema: ItemSchema.CompositeKey<T, PK, SK>,
): Table.CompositeKey<T, PK, SK> {
    val specImpl = TableSpecCompositeKeyImpl(mapper, name, schema)
    val opsImpl = TableOperationsImpl(specImpl)
    return object :
        Table.CompositeKey<T, PK, SK>,
        TableSpec.CompositeKey<T, PK, SK> by specImpl,
        TableOperations<T> by opsImpl {
        override suspend fun getItem(partitionKey: PK, sortKey: SK) = TODO("Not yet implemented")
    }
}
