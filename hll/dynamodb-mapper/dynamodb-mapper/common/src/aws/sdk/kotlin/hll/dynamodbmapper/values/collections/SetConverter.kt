/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Set] and
 * [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.List)
 * @param elementConverter A [ValueConverter] for the elements of sets
 */
public class SetConverter<T>(private val elementConverter: ValueConverter<T>) : ValueConverter<Set<T>> {
    override fun fromAv(attr: AttributeValue): Set<T> = attr.asL().map(elementConverter::fromAv).toSet()
    override fun toAv(value: Set<T>): AttributeValue = AttributeValue.L(value.map(elementConverter::toAv))
}
