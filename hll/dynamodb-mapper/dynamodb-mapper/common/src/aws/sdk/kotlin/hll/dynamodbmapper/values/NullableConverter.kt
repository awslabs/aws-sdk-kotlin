/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between potentially `null` values and
 * [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null).
 * If the value is non-null, the given [delegate] converter will be used.
 * @param A The non-nullable type
 * @param delegate A [ValueConverter] which will be used for non-null values
 */
public class NullableConverter<A : Any>(private val delegate: ValueConverter<A>) : ValueConverter<A?> {
    override fun fromAttributeValue(attr: AttributeValue): A? = when (attr) {
        is AttributeValue.Null -> null
        else -> delegate.fromAttributeValue(attr)
    }

    override fun toAttributeValue(value: A?): AttributeValue = when (value) {
        null -> AttributeValue.Null(true)
        else -> delegate.toAttributeValue(value)
    }
}
