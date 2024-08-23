/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.*
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenFrom
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapFrom
import aws.sdk.kotlin.hll.mapping.core.converters.collections.setToListConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [List] of [String] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val StringListToAttributeValueNumberSetConverter: ValueConverter<List<String>> =
    Converter(AttributeValue::Ns, AttributeValue::asNs)

/**
 * Converts between a [Set] of [String] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val StringSetToAttributeValueNumberSetConverter: ValueConverter<Set<String>> =
    StringListToAttributeValueNumberSetConverter.andThenFrom(setToListConverter())

/**
 * Creates a [ValueConverter] which converts between a [Set] of [N] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 * @param N The type of high-level values which will be converted
 */
public fun <N> numberSetConverter(numberToStringConverter: Converter<N, String>): ValueConverter<Set<N>> =
    StringSetToAttributeValueNumberSetConverter.mapFrom(numberToStringConverter)

/**
 * Converts between a [Set] of [Byte] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val ByteSetConverter: ValueConverter<Set<Byte>> = numberSetConverter(ByteToStringConverter)

/**
 * Converts between a [Set] of [Double] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val DoubleSetConverter: ValueConverter<Set<Double>> = numberSetConverter(DoubleToStringConverter)

/**
 * Converts between a [Set] of [Float] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val FloatSetConverter: ValueConverter<Set<Float>> = numberSetConverter(FloatToStringConverter)

/**
 * Converts between a [Set] of [Int] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val IntSetConverter: ValueConverter<Set<Int>> = numberSetConverter(IntToStringConverter)

/**
 * Converts between a [Set] of [Long] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val LongSetConverter: ValueConverter<Set<Long>> = numberSetConverter(LongToStringConverter)

/**
 * Converts between a [Set] of [Short] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val ShortSetConverter: ValueConverter<Set<Short>> = numberSetConverter(ShortToStringConverter)

/**
 * Converts between a [Set] of [UByte] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val UByteSetConverter: ValueConverter<Set<UByte>> = numberSetConverter(UByteToStringConverter)

/**
 * Converts between a [Set] of [UInt] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val UIntSetConverter: ValueConverter<Set<UInt>> = numberSetConverter(UIntToStringConverter)

/**
 * Converts between a [Set] of [ULong] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val ULongSetConverter: ValueConverter<Set<ULong>> = numberSetConverter(ULongToStringConverter)

/**
 * Converts between a [Set] of [UShort] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public val UShortSetConverter: ValueConverter<Set<UShort>> = numberSetConverter(UShortToStringConverter)
