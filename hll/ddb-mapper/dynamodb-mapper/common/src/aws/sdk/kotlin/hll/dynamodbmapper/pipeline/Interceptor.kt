/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline

import aws.sdk.kotlin.hll.dynamodbmapper.schemas.ItemSchema

// TODO finalize interfaces, document, add tests, construct actual request pipeline that invokes these
public interface Interceptor<I, HReq, LReq, LRes, HRes> {
    public fun readBeforeExecution(ctx: HReqContext<I, HReq>)
    public fun modifyBeforeSerialization(ctx: HReqContext<I, HReq>): SerializeInput<I, HReq>
    public fun readBeforeSerialization(ctx: HReqContext<I, HReq>)

    public fun readAfterSerialization(ctx: LReqContext<I, HReq, LReq>)
    public fun modifyBeforeInvocation(ctx: LReqContext<I, HReq, LReq>): LReq
    public fun readBeforeInvocation(ctx: LReqContext<I, HReq, LReq>)

    public fun readAfterInvocation(ctx: LResContext<I, HReq, LReq, LRes>)
    public fun modifyBeforeDeserialization(ctx: LResContext<I, HReq, LReq, LRes>): DeserializeInput<I, LReq>
    public fun readBeforeDeserialization(ctx: LResContext<I, HReq, LReq, LRes>)

    public fun readAfterDeserialization(ctx: HResContext<I, HReq, LReq, LRes, HRes>)
    public fun modifyBeforeCompletion(ctx: HResContext<I, HReq, LReq, LRes, HRes>): HReq
    public fun readAfterExecution(ctx: HResContext<I, HReq, LReq, LRes, HRes>)
}

public interface SerializeInput<I, HReq> {
    public val highLevelRequest: HReq
    public val serializeSchema: ItemSchema<I>
}

public interface HReqContext<I, HReq> : SerializeInput<I, HReq> {
    public val mapperContext: MapperContext // TBD
}

public interface LReqContext<I, HReq, LReq> : HReqContext<I, HReq> {
    public val lowLevelRequest: LReq
}

public interface DeserializeInput<I, LReq> {
    public val lowLevelResponse: LReq
    public val deserializeSchema: ItemSchema<I>
}

public interface LResContext<I, HReq, LReq, LRes> : LReqContext<I, HReq, LReq>, DeserializeInput<I, LReq>

public interface HResContext<I, HReq, LReq, LRes, HRes> : LResContext<I, HReq, LReq, LRes> {
    public val highLevelResponse: HRes
}

public interface MapperContext // TBD
