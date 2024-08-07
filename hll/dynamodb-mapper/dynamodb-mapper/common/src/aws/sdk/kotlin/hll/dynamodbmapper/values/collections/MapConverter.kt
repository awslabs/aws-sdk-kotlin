/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Map] and
 * [DynamoDB `M` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.Map)
 * @param elementConverter A [ValueConverter] for the values of maps
 */
public class MapConverter<T>(private val elementConverter: ValueConverter<T>) : ValueConverter<Map<String, T>> {
    override fun fromAv(attr: AttributeValue): Map<String, T> =
        attr.asM().mapValues { (_, v) -> elementConverter.fromAv(v) }

    override fun toAv(value: Map<String, T>): AttributeValue =
        AttributeValue.M(value.mapValues { (_, v) -> elementConverter.toAv(v) })
}
