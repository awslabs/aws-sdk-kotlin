/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenTo
import kotlin.test.Test

class NullableConverterTest : ValueConvertersTest() {
    @Test
    fun testNullConverter() = given(NullableConverter(stringReverseConverter)) {
        "foo" inDdbIs "oof"
        "bar" inDdbIs "rab"
        null inDdbIs theSame
        "null" inDdbIs "llun"
    }
}

private val stringReverseConverter = Converter(String::reversed, String::reversed).andThenTo(StringConverter)
