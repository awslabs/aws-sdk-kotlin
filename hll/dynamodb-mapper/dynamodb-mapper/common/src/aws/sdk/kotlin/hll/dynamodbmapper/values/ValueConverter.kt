/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Defines the logic for converting individual values between high-level types (e.g., [String], [Boolean], [Map]) and
 * [DynamoDB data types](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes)
 */
public interface ValueConverter<A> {
    /**
     * Convert the given [attr] from [AttributeValue] to a value of type [A]
     * @param attr The DynamoDB attribute value to convert
     * @return The value converted from [attr]
     */
    public fun fromAv(attr: AttributeValue): A

    /**
     * Convert the given [value] of type [A] to an [AttributeValue]
     * @param value The value to convert
     * @return The [AttributeValue] converted from [value]
     */
    public fun toAv(value: A): AttributeValue
}
