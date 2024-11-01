/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.DeserializeInput
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.LResContext
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.MapperContext
import aws.sdk.kotlin.hll.dynamodbmapper.util.requireNull

internal data class LResContextImpl<T, HReq, LReq, LRes>(
    override val highLevelRequest: HReq,
    override val serializeSchema: ItemSchema<T>,
    override val mapperContext: MapperContext<T>,
    override val lowLevelRequest: LReq,
    override val lowLevelResponse: LRes,
    override val deserializeSchema: ItemSchema<T>,
    override val error: Throwable?,
) : LResContext<T, HReq, LReq, LRes>,
    ErrorCombinable<LResContextImpl<T, HReq, LReq, LRes>>,
    Combinable<LResContextImpl<T, HReq, LReq, LRes>, DeserializeInput<T, LRes>> {

    override fun plus(e: Throwable?) = copy(error = e.suppressing(error))

    override fun plus(value: DeserializeInput<T, LRes>) = copy(
        lowLevelResponse = value.lowLevelResponse,
        deserializeSchema = value.deserializeSchema,
    )
}

internal operator fun <T, HReq, LReq, LRes, HRes> LResContext<T, HReq, LReq, LRes>.plus(
    highLevelResponse: HRes,
) = HResContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    lowLevelRequest,
    lowLevelResponse,
    deserializeSchema,
    highLevelResponse,
    error,
)

internal fun <T, HReq, LReq, LRes> LResContext<T, HReq, LReq, LRes?>.solidify() = LResContextImpl(
    highLevelRequest,
    serializeSchema,
    mapperContext,
    lowLevelRequest,
    requireNotNull(lowLevelResponse) { "Cannot solidify context with a null low-level response" } as LRes,
    deserializeSchema,
    requireNull(error) { "Cannot solidify context with a non-null error" },
)
