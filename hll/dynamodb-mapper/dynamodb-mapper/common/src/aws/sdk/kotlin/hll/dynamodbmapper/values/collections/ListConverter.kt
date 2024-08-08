/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [List] and
 * [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List)
 * @param elementConverter A [ValueConverter] for the elements of lists
 */
public class ListConverter<T>(private val elementConverter: ValueConverter<T>) : ValueConverter<List<T>> {
    override fun fromAttributeValue(attr: AttributeValue): List<T> =
        attr.asL().map(elementConverter::fromAttributeValue)

    override fun toAttributeValue(value: List<T>): AttributeValue =
        AttributeValue.L(value.map(elementConverter::toAttributeValue))
}
