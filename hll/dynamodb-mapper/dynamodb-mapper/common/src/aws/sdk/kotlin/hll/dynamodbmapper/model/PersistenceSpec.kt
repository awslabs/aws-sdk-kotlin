/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema

/**
 * Represents a table (optionally specialized by a secondary index) in DynamoDB and an associated item schema
 * @param T The type of objects which will be read from and/or written to this table/index
 */
public interface PersistenceSpec<T> {
    /**
     * The name of the table
     */
    public val tableName: String

    /**
     * The name of the secondary index, if any
     */
    public val indexName: String?

    /**
     * The [DynamoDbMapper] which holds the underlying DynamoDB service client used to invoke operations
     */
    public val mapper: DynamoDbMapper

    /**
     * The [ItemSchema] which describes how to map objects to items and vice versa
     */
    public val schema: ItemSchema<T>

    /**
     * A specialization of [PersistenceSpec] for a table/index with a primary key consisting of a single partition key
     */
    public interface PartitionKey<T, PK> : PersistenceSpec<T> {
        override val schema: ItemSchema.PartitionKey<T, PK>
    }

    /**
     * A specialization of [PersistenceSpec] for a table/index with a primary key that is a composite of a partition key
     * and a sort key
     */
    public interface CompositeKey<T, PK, SK> : PersistenceSpec<T> {
        override val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}
