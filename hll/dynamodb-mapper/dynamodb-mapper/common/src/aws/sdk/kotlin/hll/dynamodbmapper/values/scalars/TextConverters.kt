/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between text-like elements (e.g., strings, chars, etc.) and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public abstract class TextConverter<T : Any>(
    private val fromString: (String) -> T,
    private val toString: (T) -> String = Any::toString,
) : ValueConverter<T> {
    override fun fromAttributeValue(attr: AttributeValue): T = fromString(attr.asS())
    override fun toAttributeValue(value: T): AttributeValue = AttributeValue.S(toString(value))
}

/**
 * Converts between [CharArray] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class CharArrayConverter : TextConverter<CharArray>(String::toCharArray, ::String) {
    public companion object {
        /**
         * The default instance of [CharArrayConverter]
         */
        public val Default: CharArrayConverter = CharArrayConverter()
    }

    override fun fromAttributeValue(attr: AttributeValue): CharArray = attr.asS().toCharArray()
    override fun toAttributeValue(value: CharArray): AttributeValue = AttributeValue.S(String(value))
}

/**
 * Converts between [Char] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class CharConverter : TextConverter<Char>(String::single) {
    public companion object {
        /**
         * The default instance of [CharConverter]
         */
        public val Default: CharConverter = CharConverter()
    }
}

/**
 * Converts between [String] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class StringConverter : TextConverter<String>({ it }) {
    public companion object {
        /**
         * The default instance of [StringConverter]
         */
        public val Default: StringConverter = StringConverter()
    }
}
