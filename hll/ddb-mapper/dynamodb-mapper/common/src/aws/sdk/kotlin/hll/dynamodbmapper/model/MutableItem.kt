/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * A mutable representation of a low-level item in a DynamoDB table. Items consist of attributes, each of which have a
 * string name and a value.
 */
public interface MutableItem : MutableMap<String, AttributeValue>

/**
 * Convert this [MutableItem] to an immutable [Item]. Changes to this instance do not affect the returned instance.
 */
public fun MutableItem.toItem(): Item = toMap().toItem()

/**
 * Converts this map to a [MutableItem]
 */
public fun MutableMap<String, AttributeValue>.toMutableItem(): MutableItem = MutableItemImpl(this)

private data class MutableItemImpl(
    private val delegate: MutableMap<String, AttributeValue>,
) : MutableItem, MutableMap<String, AttributeValue> by delegate
