/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.util.attr
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConvertersTest
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.test.Test

class MapConverterTest : ValueConvertersTest() {
    @Test
    fun testMapConverter() = given(MapConverter(BarConverter)) {
        mapOf("short" to Bar(false, "meh"), "long" to Bar(true, "m", "e", "h")) inDdbIs mapOf(
            "short" to listOf(false, "meh"),
            "long" to listOf(true, "m", "e", "h"),
        )

        mapOf<String, Bar>() inDdbIs theSame
    }
}

private data class Bar(val foo: Boolean, val baz: List<String>) {
    constructor(foo: Boolean, vararg baz: String) : this(foo, baz.toList())
}

private object BarConverter : ValueConverter<Bar> {
    override fun convertFrom(to: AttributeValue): Bar {
        val list = to.asL()
        val foo = list.first().asBool()
        val baz = list.drop(1).map { it.asS() }
        return Bar(foo, baz)
    }

    override fun convertTo(from: Bar) = AttributeValue.L(listOf(attr(from.foo)) + from.baz.map(::attr))
}
