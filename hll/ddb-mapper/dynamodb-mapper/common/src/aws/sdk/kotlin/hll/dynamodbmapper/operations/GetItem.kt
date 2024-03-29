/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations

import aws.sdk.kotlin.hll.dynamodbmapper.TableSpec
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.HReqContextImpl
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.MapperContextImpl
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal.Operation
import aws.sdk.kotlin.services.dynamodb.model.ConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest as LowLevelGetItemRequest
import aws.sdk.kotlin.services.dynamodb.model.GetItemResponse as LowLevelGetItemResponse

// TODO Replace everything in this file with codegenned request/response types, converters, operation factory, etc.

public interface GetItemRequest<T> {
    public companion object {
        public operator fun <T> invoke(
            key: T,
            consistentRead: Boolean? = null,
            returnConsumedCapacity: ReturnConsumedCapacity? = null,
        ): GetItemRequest<T> = object : GetItemRequest<T> {
            override val key = key
            override val consistentRead = consistentRead
            override val returnConsumedCapacity = returnConsumedCapacity
        }
    }

    public val consistentRead: Boolean?
    public val key: T
    public val returnConsumedCapacity: ReturnConsumedCapacity?
}

private fun <T> GetItemRequest<T>.convert(tableName: String, schema: ItemSchema<T>) = LowLevelGetItemRequest {
    consistentRead = this@convert.consistentRead
    key = schema.converter.toItem(this@convert.key, schema.keyAttributeNames)
    returnConsumedCapacity = this@convert.returnConsumedCapacity
    this.tableName = tableName
}

public interface GetItemResponse<T> {
    public val consumedCapacity: ConsumedCapacity?
    public val item: T?
}

private data class GetItemResponseImpl<T>(
    override val consumedCapacity: ConsumedCapacity?,
    override val item: T?,
) : GetItemResponse<T>

private fun <T> LowLevelGetItemResponse.convert(schema: ItemSchema<T>): GetItemResponse<T> =
    GetItemResponseImpl(
        consumedCapacity = consumedCapacity,
        item = item?.toItem()?.let(schema.converter::fromItem),
    )

internal fun <T> getItemOperation(table: TableSpec<T>) = Operation(
    initialize = { hReq: GetItemRequest<T> -> HReqContextImpl(hReq, table.schema, MapperContextImpl(table, "GetItem")) },
    serialize = { hReq, schema -> hReq.convert(table.name, schema) },
    lowLevelInvoke = table.mapper.client::getItem,
    deserialize = LowLevelGetItemResponse::convert,
    interceptors = table.mapper.config.interceptors,
)
