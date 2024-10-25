/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenTo
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Namespace for containing various conversion utilities dealing with text conversion
 */
@ExperimentalApi
public object TextConverters {
    /**
     * Converts between [CharArray] and [String]
     */
    public val CharArrayToStringConverter: Converter<CharArray, String> = Converter(CharArray::concatToString, String::toCharArray)

    /**
     * Converts between [Char] and [String]
     */
    public val CharToStringConverter: Converter<Char, String> = Converter(Char::toString, String::single)

    /**
     * Converts between [String] and [String]
     */
    public val StringToStringConverter: Converter<String, String> = Converter({ it }, { it })
}

/**
 * Converts between [String] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val StringConverter: ValueConverter<String> = Converter(AttributeValue::S, AttributeValue::asS)

/**
 * Converts between [CharArray] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val CharArrayConverter: ValueConverter<CharArray> =
    TextConverters.CharArrayToStringConverter.andThenTo(StringConverter)

/**
 * Converts between [Char] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
@ExperimentalApi
public val CharConverter: ValueConverter<Char> = TextConverters.CharToStringConverter.andThenTo(StringConverter)
