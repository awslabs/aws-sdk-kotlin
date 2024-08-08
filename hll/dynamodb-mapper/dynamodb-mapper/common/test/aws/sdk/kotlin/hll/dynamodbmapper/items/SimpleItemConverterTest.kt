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
    fun testSomething() {
        val converter = SimpleItemConverter(
            ::ProductBuilder,
            ProductBuilder::build,
            AttributeDescriptor("id", Product::id, ProductBuilder::id::set, IntConverter.Default),
            AttributeDescriptor("name", Product::name, ProductBuilder::name::set, StringConverter.Default),
            AttributeDescriptor("in-stock", Product::inStock, ProductBuilder::inStock::set, BooleanConverter.Default),
        )

        val foo = Product(42, "Foo 2.0", inStock = true)
        val item = converter.toItem(foo)

        assertEquals(3, item.size)
        assertEquals(42, item.getValue("id").asN().toInt())
        assertEquals("Foo 2.0", item.getValue("name").asS())
        assertTrue(item.getValue("in-stock").asBool())

        val unconverted = converter.fromItem(item)
        assertEquals(foo, unconverted)
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
