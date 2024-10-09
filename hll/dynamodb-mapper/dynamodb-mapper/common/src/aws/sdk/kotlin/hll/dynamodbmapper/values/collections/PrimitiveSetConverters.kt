/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.TextConverters
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenFrom
import aws.sdk.kotlin.hll.mapping.core.converters.collections.CollectionTypeConverters
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapFrom
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between a [Set] of [ByteArray] elements and
 * [DynamoDB `BS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val ByteArraySetConverter: ValueConverter<Set<ByteArray>> = Converter(
    convertTo = { from: Set<ByteArray> -> AttributeValue.Bs(from.toList()) },
    convertFrom = { to: AttributeValue -> to.asBs().toSet() },
)

/**
 * Converts between a [List] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val StringListToAttributeValueStringSetConverter: ValueConverter<List<String>> =
    Converter(AttributeValue::Ss, AttributeValue::asSs)

/**
 * Converts between a [Set] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val StringSetConverter: ValueConverter<Set<String>> =
    StringListToAttributeValueStringSetConverter.andThenFrom(CollectionTypeConverters.SetToListConverter())

/**
 * Converts between a [Set] of [CharArray] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val CharArraySetConverter: ValueConverter<Set<CharArray>> =
    StringSetConverter.mapFrom(TextConverters.CharArrayToStringConverter)

/**
 * Converts between a [Set] of [Char] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
@ExperimentalApi
public val CharSetConverter: ValueConverter<Set<Char>> =
    StringSetConverter.mapFrom(TextConverters.CharToStringConverter)
