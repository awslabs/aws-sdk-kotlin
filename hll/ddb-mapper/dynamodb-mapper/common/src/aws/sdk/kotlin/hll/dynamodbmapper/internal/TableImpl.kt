/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.Table
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemResponse
import aws.sdk.kotlin.hll.dynamodbmapper.operations.getItemOperation
import kotlinx.coroutines.flow.Flow

// TODO move operation implementations to codegen

internal abstract class TableImpl<T>(override val mapper: DynamoDbMapper, override val name: String) : Table<T> {
    override suspend fun getItem(request: GetItemRequest<T>): GetItemResponse<T> =
        getItemOperation(this).execute(request)

    override suspend fun putItem(obj: T) = TODO("not yet implemented")

    override fun scan(): Flow<T> = TODO("not yet implemented")

    internal class PartitionKeyImpl<T, PK> internal constructor(
        mapper: DynamoDbMapper,
        name: String,
        override val schema: ItemSchema.PartitionKey<T, PK>,
    ) : TableImpl<T>(mapper, name), Table.PartitionKey<T, PK> {
        override suspend fun getItem(partitionKey: PK) = TODO("not yet implemented")
    }

    internal class CompositeKeyImpl<T, PK, SK> internal constructor(
        mapper: DynamoDbMapper,
        name: String,
        override val schema: ItemSchema.CompositeKey<T, PK, SK>,
    ) : TableImpl<T>(mapper, name), Table.CompositeKey<T, PK, SK> {
        override suspend fun getItem(partitionKey: PK, sortKey: SK) = TODO("not yet implemented")
    }
}
