/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of text-like elements (e.g., strings, chars, etc.) and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 * @param T The type of high-level values which will be converted
 */
public abstract class TextSetConverter<T : Any>(
    private val fromString: (String) -> T,
    private val toString: (T) -> String = Any::toString,
) : ValueConverter<Set<T>> {
    override fun fromAttributeValue(attr: AttributeValue): Set<T> = attr.asSs().map(fromString).toSet()
    override fun toAttributeValue(value: Set<T>): AttributeValue = AttributeValue.Ss(value.map(toString))
}

/**
 * Converts between a [Set] of [CharArray] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class CharArraySetConverter : TextSetConverter<CharArray>(String::toCharArray, ::String) {
    public companion object {
        /**
         * The default instance of [CharArraySetConverter]
         */
        public val Default: CharArraySetConverter = CharArraySetConverter()
    }
}

/**
 * Converts between a [Set] of [Char] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class CharSetConverter : TextSetConverter<Char>(String::single) {
    public companion object {
        /**
         * The default instance of [CharSetConverter]
         */
        public val Default: CharSetConverter = CharSetConverter()
    }
}

/**
 * Converts between a [Set] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class StringSetConverter : TextSetConverter<String>({ it }) {
    public companion object {
        /**
         * The default instance of [StringSetConverter]
         */
        public val Default: StringSetConverter = StringSetConverter()
    }
}
