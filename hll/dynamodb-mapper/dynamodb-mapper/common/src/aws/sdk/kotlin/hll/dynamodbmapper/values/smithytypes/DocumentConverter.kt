/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.NullableConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.ListConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.collections.MapConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.AutoNumberConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringConverter
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapFrom
import aws.sdk.kotlin.hll.mapping.core.converters.collections.mapValuesFrom
import aws.sdk.kotlin.hll.mapping.core.converters.mergeBy
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi
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
@ExperimentalApi
public class DocumentConverter(
    private val numberConverter: ValueConverter<Number> = AutoNumberConverter,
    private val stringConverter: ValueConverter<String> = StringConverter,
    private val booleanConverter: ValueConverter<Boolean> = BooleanConverter,
    nullableConverter: NullableConverter<Document> = NullableConverter(),
    listConverter: ValueConverter<List<AttributeValue>> = ListConverter,
    mapConverter: ValueConverter<Map<String, AttributeValue>> = MapConverter,
) : ValueConverter<Document> {
    private val nullableConverter = nullableConverter.mergeBy(this)
    private val listConverter = listConverter.mapFrom(this.nullableConverter)
    private val mapConverter = mapConverter.mapValuesFrom(this.nullableConverter)

    @ExperimentalApi
    public companion object {
        /**
         * The default instance of [DocumentConverter]
         */
        public val Default: DocumentConverter = DocumentConverter()
    }

    override fun convertFrom(to: AttributeValue): Document = when (to) {
        is AttributeValue.N -> Document.Number(numberConverter.convertFrom(to))
        is AttributeValue.S -> Document.String(stringConverter.convertFrom(to))
        is AttributeValue.Bool -> Document.Boolean(booleanConverter.convertFrom(to))
        is AttributeValue.L -> Document.List(listConverter.convertFrom(to))
        is AttributeValue.M -> Document.Map(mapConverter.convertFrom(to))
        else -> throw IllegalArgumentException("Documents do not support ${to::class.qualifiedName} values")
    }

    override fun convertTo(from: Document): AttributeValue = when (from) {
        is Document.Number -> numberConverter.convertTo(from.value)
        is Document.String -> stringConverter.convertTo(from.value)
        is Document.Boolean -> booleanConverter.convertTo(from.value)
        is Document.List -> listConverter.convertTo(from.value)
        is Document.Map -> mapConverter.convertTo(from.value)
    }
}
