/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema

/**
 * Represents a table in DynamoDB and an associated item schema.
 * @param T The type of objects which will be read from and/or written to this table
 */
public interface TableSpec<T> {
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

    public interface PartitionKey<T, PK> : TableSpec<T> {
        override val schema: ItemSchema.PartitionKey<T, PK>
    }

    public interface CompositeKey<T, PK, SK> : TableSpec<T> {
        override val schema: ItemSchema.CompositeKey<T, PK, SK>
    }
}
