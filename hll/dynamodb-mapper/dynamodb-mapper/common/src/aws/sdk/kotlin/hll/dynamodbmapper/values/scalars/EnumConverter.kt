/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Enum] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class EnumConverter<E : Enum<E>>(private val fromString: (String) -> E) : ValueConverter<E> {
    public companion object {
        /**
         * Creates a new [EnumConverter] for the enum type [E]
         * @param E the enum type for which to create an [EnumConverter]
         */
        public inline operator fun <reified E : Enum<E>> invoke(): EnumConverter<E> = EnumConverter(::enumValueOf)
    }

    override fun fromAttributeValue(attr: AttributeValue): E = fromString(attr.asS())
    override fun toAttributeValue(value: E): AttributeValue = AttributeValue.S(value.name)
}
