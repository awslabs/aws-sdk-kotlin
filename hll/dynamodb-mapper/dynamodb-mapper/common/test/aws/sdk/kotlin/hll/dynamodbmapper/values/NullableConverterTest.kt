/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test

class NullableConverterTest : ValueConvertersTest() {
    @Test
    fun testNullConverter() = given(NullableConverter(StringReverseConverter)) {
        "foo" inDdbIs "oof"
        "bar" inDdbIs "rab"
        null inDdbIs theSame
        "null" inDdbIs "llun"
    }
}

private object StringReverseConverter : ValueConverter<String> {
    override fun fromAv(attr: AttributeValue) = attr.asS().reversed()
    override fun toAv(value: String) = AttributeValue.S(value.reversed())
}
