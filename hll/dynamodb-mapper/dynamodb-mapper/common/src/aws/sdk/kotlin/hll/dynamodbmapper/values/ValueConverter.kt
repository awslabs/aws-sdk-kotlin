/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Defines the logic for converting individual values between a high-level type [V] (e.g., [String], [Boolean], [Map])
 * and
 * [DynamoDB data types](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes)
 * @param V The type of high-level values which will be converted to low-level DynamoDB attribute values
 */
public interface ValueConverter<V> {
    /**
     * Convert the given [attr] from an [AttributeValue] to a value of type [V]
     * @param attr The DynamoDB attribute value to convert
     * @return The value converted from [attr]
     */
    public fun fromAttributeValue(attr: AttributeValue): V

    /**
     * Convert the given [value] of type [V] to an [AttributeValue]
     * @param value The value to convert
     * @return The [AttributeValue] converted from [value]
     */
    public fun toAttributeValue(value: V): AttributeValue
}
