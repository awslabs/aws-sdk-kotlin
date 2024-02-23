/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper.items

import aws.sdk.kotlin.hlls.dynamodbmapper.model.Item
import aws.sdk.kotlin.hlls.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.hlls.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public class SimpleItemConverter<I, B>(
    private val builderFactory: () -> B,
    private val build: B.() -> I,
    private val descriptors: Collection<AttributeDescriptor<*, I, B>>,
) : ItemConverter<I> {
    override fun fromItem(item: Item): I {
        val builder = builderFactory()

        fun <A> AttributeDescriptor<A, I, B>.fromAv(av: AttributeValue) =
            builder.setter(converter.fromAv(av))

        item.forEach { (key, av) ->
            descriptors.single { it.key == key }.fromAv(av)
        }

        return builder.build()
    }

    override fun toItem(obj: I, onlyKeys: Set<String>?): Item {
        fun <A> AttributeDescriptor<A, I, B>.toAv() =
            converter.toAv(getter(obj))

        val attributes = if (onlyKeys == null) {
            this.descriptors
        } else {
            this.descriptors.filter { it.key in onlyKeys }
        }

        return buildItem {
            attributes.forEach { attr -> put(attr.key, attr.toAv()) }
        }
    }
}

public class AttributeDescriptor<A, I, B>(
    public val key: String,
    public val getter: (I) -> A,
    public val setter: B.(A) -> Unit,
    public val converter: ValueConverter<A>,
)
