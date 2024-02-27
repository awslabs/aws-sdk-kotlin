/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item

// TODO document, add unit tests
public interface ItemConverter<I> {
    public fun fromItem(item: Item): I
    public fun toItem(obj: I, onlyKeys: Set<String>? = null): Item
}
