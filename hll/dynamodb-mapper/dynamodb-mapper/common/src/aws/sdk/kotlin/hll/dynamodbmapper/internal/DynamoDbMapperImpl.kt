/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.internal

import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbMapper
import aws.sdk.kotlin.hll.dynamodbmapper.items.ItemSchema
import aws.sdk.kotlin.hll.dynamodbmapper.model.internal.tableImpl
import aws.sdk.kotlin.hll.dynamodbmapper.pipeline.InterceptorAny
import aws.sdk.kotlin.runtime.http.interceptors.AwsBusinessMetric
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.withConfig
import aws.smithy.kotlin.runtime.businessmetrics.emitBusinessMetric
import aws.smithy.kotlin.runtime.client.RequestInterceptorContext
import aws.smithy.kotlin.runtime.http.interceptors.HttpInterceptor

internal data class DynamoDbMapperImpl(
    override val client: DynamoDbClient,
    override val config: DynamoDbMapper.Config,
) : DynamoDbMapper {
    internal companion object {
        /**
         * Wraps a low-level [DynamoDbClient] to add additional features before instantiating a new
         * [DynamoDbMapperImpl].
         */
        fun wrapping(client: DynamoDbClient, config: DynamoDbMapper.Config): DynamoDbMapperImpl {
            val wrappedClient = client.withConfig { interceptors += DdbMapperMetricInterceptor }
            return DynamoDbMapperImpl(wrappedClient, config)
        }
    }
    override fun <T, PK> getTable(name: String, schema: ItemSchema.PartitionKey<T, PK>) =
        tableImpl(this, name, schema)

    override fun <T, PK, SK> getTable(name: String, schema: ItemSchema.CompositeKey<T, PK, SK>) =
        tableImpl(this, name, schema)
}

internal data class MapperConfigImpl(
    override val interceptors: List<InterceptorAny>,
) : DynamoDbMapper.Config {
    override fun toBuilder() = DynamoDbMapper
        .Config
        .Builder()
        .also { it.interceptors = interceptors.toMutableList() }
}

internal class MapperConfigBuilderImpl : DynamoDbMapper.Config.Builder {
    override var interceptors = mutableListOf<InterceptorAny>()

    override fun build() = MapperConfigImpl(interceptors.toList())
}

/**
 * An interceptor that emits the DynamoDB Mapper business metric
 */
private object DdbMapperMetricInterceptor : HttpInterceptor {
    override suspend fun modifyBeforeSerialization(context: RequestInterceptorContext<Any>): Any {
        context.executionContext.emitBusinessMetric(AwsBusinessMetric.DDB_MAPPER)
        return context.request
    }
}
