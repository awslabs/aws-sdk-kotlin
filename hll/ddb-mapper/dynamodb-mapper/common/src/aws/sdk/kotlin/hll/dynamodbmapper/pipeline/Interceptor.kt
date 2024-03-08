/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema

// TODO finalize interfaces, document, add tests, construct actual request pipeline that invokes these
public interface Interceptor<T, HReq, LReq, LRes, HRes> {
    public fun readBeforeExecution(ctx: HReqContext<T, HReq>)
    public fun modifyBeforeSerialization(ctx: HReqContext<T, HReq>): SerializeInput<T, HReq>
    public fun readBeforeSerialization(ctx: HReqContext<T, HReq>)

    public fun readAfterSerialization(ctx: LReqContext<T, HReq, LReq>)
    public fun modifyBeforeInvocation(ctx: LReqContext<T, HReq, LReq>): LReq
    public fun readBeforeInvocation(ctx: LReqContext<T, HReq, LReq>)

    public fun readAfterInvocation(ctx: LResContext<T, HReq, LReq, LRes>)
    public fun modifyBeforeDeserialization(ctx: LResContext<T, HReq, LReq, LRes>): DeserializeInput<T, LReq>
    public fun readBeforeDeserialization(ctx: LResContext<T, HReq, LReq, LRes>)

    public fun readAfterDeserialization(ctx: HResContext<T, HReq, LReq, LRes, HRes>)
    public fun modifyBeforeCompletion(ctx: HResContext<T, HReq, LReq, LRes, HRes>): HReq
    public fun readAfterExecution(ctx: HResContext<T, HReq, LReq, LRes, HRes>)
}

public interface SerializeInput<T, HReq> {
    public val highLevelRequest: HReq
    public val serializeSchema: ItemSchema<T>
}

public interface HReqContext<T, HReq> : SerializeInput<T, HReq> {
    public val mapperContext: MapperContext // TBD
}

public interface LReqContext<T, HReq, LReq> : HReqContext<T, HReq> {
    public val lowLevelRequest: LReq
}

public interface DeserializeInput<T, LReq> {
    public val lowLevelResponse: LReq
    public val deserializeSchema: ItemSchema<T>
}

public interface LResContext<T, HReq, LReq, LRes> : LReqContext<T, HReq, LReq>, DeserializeInput<T, LReq>

public interface HResContext<T, HReq, LReq, LRes, HRes> : LResContext<T, HReq, LReq, LRes> {
    public val highLevelResponse: HRes
}

public interface MapperContext // TBD
