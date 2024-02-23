/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public class Item(private val delegate: Map<String, AttributeValue>) : Map<String, AttributeValue> by delegate {
    override fun equals(other: Any?): Boolean = other is Item && delegate == other.delegate

    override fun hashCode(): Int = delegate.hashCode()

    public fun toMutableItem(): MutableItem = MutableItem(delegate.toMutableMap())
}

public fun buildItem(block: MutableItem.() -> Unit): Item = MutableItem(mutableMapOf()).apply(block).toItem()

public fun itemOf(vararg pairs: Pair<String, AttributeValue>): Item = Item(mapOf(*pairs))

public fun Map<String, AttributeValue>.toItem(): Item = Item(this)
