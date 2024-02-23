/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public class MutableItem(
    private val delegate: MutableMap<String, AttributeValue>,
) : MutableMap<String, AttributeValue> by delegate {
    public fun toItem(): Item = Item(delegate.toMap())

    override fun equals(other: Any?): Boolean = other is MutableItem && delegate == other.delegate

    override fun hashCode(): Int = delegate.hashCode()

    public fun toMutableItem(): MutableItem = MutableItem(delegate.toMutableMap())
}

public fun MutableMap<String, AttributeValue>.toMutableItem(): MutableItem = MutableItem(this)
