/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenFrom

/**
 * Creates a [Converter] between enum type [E] and [String]
 * @param E The [Enum] type to convert
 */
public inline fun <reified E : Enum<E>> enumToStringConverter(): Converter<E, String> = Converter(
    convertTo = { from: E -> from.name },
    convertFrom = { to: String -> enumValueOf(to) },
)

/**
 * Converts between [Enum] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 * @param E The [Enum] type to convert
 */
public class EnumConverter<E : Enum<E>>(
    private val enumToStringConverter: Converter<E, String>,
) : ValueConverter<E> by StringConverter.andThenFrom(enumToStringConverter)

/**
 * Instantiates a new [ValueConverter] for enums of type [E]
 * @param E The [Enum] type for which to create a [ValueConverter]
 */
public inline fun <reified E : Enum<E>> EnumConverter(): EnumConverter<E> =
    EnumConverter(enumToStringConverter<E>())
