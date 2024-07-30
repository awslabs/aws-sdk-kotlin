/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.hll.dynamodbmapper.operations.QueryableOperations

/**
 * Represents a source of data which may be queried, such as a table or secondary index
 */
public interface Queryable<T> :
    PersistenceSpec<T>,
    QueryableOperations<T> {

    /**
     * Represents a table/index whose primary key is a single partition key
     * @param T The type of objects which will be read from and/or written to this table/index
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     */
    public interface PartitionKey<T, PK> :
        Queryable<T>,
        PersistenceSpec.PartitionKey<T, PK>

    /**
     * Represents a table/index whose primary key is a composite of a partition key and a sort key
     * @param T The type of objects which will be read from and/or written to this table/index
     * @param PK The type of the partition key property, either [String], [Number], or [ByteArray]
     * @param SK The type of the sort key property, either [String], [Number], or [ByteArray]
     */
    public interface CompositeKey<T, PK, SK> :
        Queryable<T>,
        PersistenceSpec.CompositeKey<T, PK, SK>
}
