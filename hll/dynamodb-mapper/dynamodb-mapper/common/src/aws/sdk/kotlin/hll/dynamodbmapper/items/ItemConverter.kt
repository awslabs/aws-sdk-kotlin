/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item

/**
 * Defines the logic for converting between objects and DynamoDB items
 * @param T The type of objects which will be converted
 */
public interface ItemConverter<T> {
    /**
     * Convert the given [item] to an object of type [T]
     * @param item The item to convert to an object
     * @return The object converted from [item]
     */
    public fun fromItem(item: Item): T

    /**
     * Convert the given [obj] of type [T] to an [Item]
     * @param obj The object to convert to an item
     * @param onlyAttributes Limit the attributes which are set in the item to those named. If not set, converts all
     * attributes.
     * @return The item converted from [obj]
     */
    public fun toItem(obj: T, onlyAttributes: Set<String>? = null): Item
}
