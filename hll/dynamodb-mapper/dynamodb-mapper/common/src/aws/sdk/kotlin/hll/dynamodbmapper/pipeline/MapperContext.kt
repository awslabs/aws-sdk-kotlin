/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.MapperContextImpl

/**
 * Holds generalized context which may be useful to interceptors
 * @param T The type of objects being converted to/from DynamoDB items
 */
public interface MapperContext<T> {
    // TODO what other fields would be useful in here?

    /**
     * The metadata for a table (e.g., name) involved in the operation
     */
    public val tableSpec: TableSpec<T>

    /**
     * The name of the high-level operation being invoked
     */
    public val operation: String
}

/**
 * Create a new [MapperContext]
 * @param T The type of objects being converted to/from DynamoDB items
 * @param tableSpec The metadata for a table (e.g., name) involved in the operation
 * @param operation The name of the high-level operation being invoked
 */
public fun <T> MapperContext(tableSpec: TableSpec<T>, operation: String): MapperContext<T> =
    MapperContextImpl(tableSpec, operation)
