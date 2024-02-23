/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hlls.dynamodbmapper.items

import aws.sdk.kotlin.hlls.dynamodbmapper.model.Item

public interface ItemConverter<I> {
    public fun fromItem(item: Item): I
    public fun toItem(obj: I, onlyKeys: Set<String>? = null): Item
}
