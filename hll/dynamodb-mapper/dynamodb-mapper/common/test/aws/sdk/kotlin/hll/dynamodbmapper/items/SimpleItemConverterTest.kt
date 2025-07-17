/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.BooleanConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.IntConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleItemConverterTest {
    @Test
    fun testBasicConversion() {
        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, IntConverter),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringConverter),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanConverter),
        )

        val foo = Product(42, "Foo 2.0", inStock = true)
        val item = converter.convertTo(foo)

        assertEquals(3, item.size)
        assertEquals(42, item.getValue("id").asN().toInt())
        assertEquals("Foo 2.0", item.getValue("name").asS())
        assertTrue(item.getValue("in-stock").asBool())

        val unconverted = converter.convertFrom(item)
        assertEquals(foo, unconverted)
    }

    @Test
    fun testKeyOnlyConversion() {
        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, IntConverter),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringConverter),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanConverter),
        )

        val foo = Product(42, "Foo 2.0", inStock = true)
        val item = converter.convertTo(foo, setOf("id", "name"))

        assertEquals(2, item.size)
        assertEquals(42, item.getValue("id").asN().toInt())
        assertEquals("Foo 2.0", item.getValue("name").asS())
    }
}

private data class Product(
    val id: Int,
    val name: String,
    val inStock: Boolean,
)

private class ProductBuilder {
    var id: Int? = null
    var name: String? = null
    var inStock: Boolean? = null

    fun build() = Product(id!!, name!!, inStock!!)
}
