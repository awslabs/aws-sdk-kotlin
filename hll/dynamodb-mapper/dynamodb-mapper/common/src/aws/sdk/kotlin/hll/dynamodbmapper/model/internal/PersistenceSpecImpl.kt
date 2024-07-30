/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.PersistenceSpec

internal data class PersistenceSpecPartitionKeyImpl<T, PK>(
    override val mapper: DynamoDbMapper,
    override val tableName: String,
    override val indexName: String?,
    override val schema: ItemSchema.PartitionKey<T, PK>,
) : PersistenceSpec.PartitionKey<T, PK>

internal data class PersistenceSpecCompositeKeyImpl<T, PK, SK>(
    override val mapper: DynamoDbMapper,
    override val tableName: String,
    override val indexName: String?,
    override val schema: ItemSchema.CompositeKey<T, PK, SK>,
) : PersistenceSpec.CompositeKey<T, PK, SK>
