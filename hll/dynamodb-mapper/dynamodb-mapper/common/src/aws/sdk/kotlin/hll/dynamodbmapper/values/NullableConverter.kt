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
 * @param delegate A [ValueConverter] which will be used for non-null values
 */
public class NullableConverter<T : Any>(private val delegate: ValueConverter<T>) : ValueConverter<T?> {
    override fun fromAv(attr: AttributeValue): T? = when (attr) {
        is AttributeValue.Null -> null
        else -> delegate.fromAv(attr)
    }

    override fun toAv(value: T?): AttributeValue = when (value) {
        null -> AttributeValue.Null(true)
        else -> delegate.toAv(value)
    }
}
