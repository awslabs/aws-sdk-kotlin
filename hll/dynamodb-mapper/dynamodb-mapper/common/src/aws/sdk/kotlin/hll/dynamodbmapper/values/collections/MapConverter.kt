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
 * Note that this converter requires maps' keys to be strings to in order to match DynamoDB's `M` type. Maps with
 * non-string keys must use a custom converter.
 * @param elementConverter A [ValueConverter] used to convert the values inside maps
 */
public class MapConverter<T>(private val elementConverter: ValueConverter<T>) : ValueConverter<Map<String, T>> {
    override fun fromAttributeValue(attr: AttributeValue): Map<String, T> =
        attr.asM().mapValues { (_, v) -> elementConverter.fromAttributeValue(v) }

    override fun toAttributeValue(value: Map<String, T>): AttributeValue =
        AttributeValue.M(value.mapValues { (_, v) -> elementConverter.toAttributeValue(v) })
}
