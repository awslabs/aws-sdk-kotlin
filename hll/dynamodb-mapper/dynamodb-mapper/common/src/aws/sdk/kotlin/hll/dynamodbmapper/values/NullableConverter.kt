/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.util.NULL_ATTR
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.SplittingConverter
import aws.sdk.kotlin.hll.mapping.core.converters.mergeBy
import aws.sdk.kotlin.hll.mapping.core.util.Either
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi
import kotlin.reflect.KClass

/**
 * Converts between potentially `null` values and
 * [DynamoDB `NULL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Null).
 * Note that this class is a [SplittingConverter] and the logic for handling non-null values is undefined in this class.
 * Thus, it is typically used in conjunction with the [NullableConverter] factory function or via [mergeBy].
 * @param V The non-nullable type
 */
@ExperimentalApi
public class NullableConverter<V : Any>(klass: KClass<V>) : SplittingConverter<V?, V, AttributeValue, AttributeValue> {
    override fun convertTo(from: V?): Either<AttributeValue, V> = when (from) {
        null -> Either.Left(NULL_ATTR)
        else -> Either.Right(from)
    }

    override fun convertFrom(to: AttributeValue): Either<V?, AttributeValue> = when (to) {
        is AttributeValue.Null -> Either.Left(null)
        else -> Either.Right(to)
    }
}

/**
 * Initializes a new [NullableConverter] for the given reified type [V]
 */
@ExperimentalApi
public inline fun <reified V : Any> NullableConverter(): NullableConverter<V> = NullableConverter(V::class)

@ExperimentalApi
@Suppress("ktlint:standard:function-naming")
public inline fun <reified F : Any> NullableConverter(
    delegate: Converter<F, AttributeValue>,
): Converter<F?, AttributeValue> = NullableConverter<F>().mergeBy(delegate)
