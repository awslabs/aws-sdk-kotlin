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

class ListConverterTest : ValueConvertersTest() {
    @Test
    fun testListConverter() = given(ListConverter(FooConverter)) {
        listOf(Foo("apple", 1), Foo("banana", 2), Foo("cherry", 3)) inDdbIs listOf(
            mapOf("bar" to "apple", "baz" to 1),
            mapOf("bar" to "banana", "baz" to 2),
            mapOf("bar" to "cherry", "baz" to 3),
        )

        List(3) { Foo("date", 4) } inDdbIs List(3) { mapOf("bar" to "date", "baz" to 4) }

        listOf<Foo>() inDdbIs theSame
    }
}

private data class Foo(val bar: String, val baz: Int)

private object FooConverter : ValueConverter<Foo> {
    override fun convertFrom(to: AttributeValue): Foo {
        val map = to.asM()
        val bar = map.getValue("bar").asS()
        val baz = map.getValue("baz").asN().toInt()
        return Foo(bar, baz)
    }

    override fun convertTo(from: Foo) = AttributeValue.M(
        mapOf(
            "bar" to attr(from.bar),
            "baz" to attr(from.baz),
        ),
    )
}
