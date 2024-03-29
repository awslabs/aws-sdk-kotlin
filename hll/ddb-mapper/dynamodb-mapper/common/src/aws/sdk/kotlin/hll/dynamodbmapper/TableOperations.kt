/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper

import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemRequest
import aws.sdk.kotlin.hll.dynamodbmapper.operations.GetItemResponse
import kotlinx.coroutines.flow.Flow

/**
 * Provides access to operations on a particular table, which will invoke low-level operations after mapping objects to
 * items and vice versa
 * @param T The type of objects which will be read from and/or written to this table
 */
public interface TableOperations<T> {
    // TODO reimplement operations to be codegenned and add extension functions where appropriate, docs, etc.
    public suspend fun getItem(request: GetItemRequest<T>): GetItemResponse<T>
    public suspend fun putItem(obj: T)
    public fun scan(): Flow<T>
}
