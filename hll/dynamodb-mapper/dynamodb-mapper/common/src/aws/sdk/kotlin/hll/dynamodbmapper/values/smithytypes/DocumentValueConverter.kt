/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.NullableConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.ListConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.MapConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.content.Document

/**
 * Converts between [Document] and various DynamoDB value types. The following conversions are performed:
 * * `null` ↔ [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null)
 * * [Document.Number] ↔ [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 * * [Document.String] ↔ [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 * * [Document.Boolean] ↔ [DynamoDB `BOOL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Boolean)
 * * [Document.List] ↔ [DynamoDB `L` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.List)
 * * [Document.Map] ↔ [DynamoDB `M` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Document.Map)
 */
public class DocumentValueConverter(
    private val numberConverter: ValueConverter<Number>,
    private val stringConverter: ValueConverter<String>,
    private val booleanConverter: ValueConverter<Boolean>,
    nullConverterFactory: (ValueConverter<Document>) -> ValueConverter<Document?>,
    listConverterFactory: (ValueConverter<Document?>) -> ValueConverter<List<Document?>>,
    mapConverterFactory: (ValueConverter<Document?>) -> ValueConverter<Map<String, Document?>>,
) : ValueConverter<Document> {
    private val nullConverter = nullConverterFactory(this)
    private val listConverter = listConverterFactory(nullConverter)
    private val mapConverter = mapConverterFactory(nullConverter)

    public companion object {
        /**
         * The default [ValueConverter] to use for [Document.Number] values. When converting attribute values into
         * values, the following concrete subclasses of [Number] will be returned:
         * * [Double] — If the number contains any fractional component
         * * [Int] — If the number is in the range of [Int.MIN_VALUE] and [Int.MAX_VALUE] (inclusive)
         * * [Long] — Anything else
         */
        public val DefaultNumberConverter: ValueConverter<Number> = object : ValueConverter<Number> {
            override fun fromAv(attr: AttributeValue): Number {
                val numberString = attr.asN()
                return when {
                    '.' in numberString -> numberString.toDouble()
                    else -> when (val longNumber = numberString.toLong()) {
                        in Int.MIN_VALUE..Int.MAX_VALUE -> longNumber.toInt()
                        else -> longNumber
                    }
                }
            }

            override fun toAv(value: Number): AttributeValue = AttributeValue.N(value.toString())
        }

        /**
         * The default instance of [DocumentValueConverter]
         */
        public val Default: DocumentValueConverter = DocumentValueConverter(
            numberConverter = DefaultNumberConverter,
            stringConverter = StringConverter.Default,
            booleanConverter = BooleanConverter.Default,
            nullConverterFactory = ::NullableConverter,
            listConverterFactory = ::ListConverter,
            mapConverterFactory = ::MapConverter,
        )
    }

    override fun fromAv(attr: AttributeValue): Document = when (attr) {
        is AttributeValue.N -> Document.Number(numberConverter.fromAv(attr))
        is AttributeValue.S -> Document.String(stringConverter.fromAv(attr))
        is AttributeValue.Bool -> Document.Boolean(booleanConverter.fromAv(attr))
        is AttributeValue.L -> Document.List(listConverter.fromAv(attr))
        is AttributeValue.M -> Document.Map(mapConverter.fromAv(attr))
        else -> throw IllegalArgumentException("Documents do not support ${attr::class.qualifiedName} values")
    }

    override fun toAv(value: Document): AttributeValue = when (value) {
        is Document.Number -> numberConverter.toAv(value.value)
        is Document.String -> stringConverter.toAv(value.value)
        is Document.Boolean -> booleanConverter.toAv(value.value)
        is Document.List -> listConverter.toAv(value.value)
        is Document.Map -> mapConverter.toAv(value.value)
    }
}
