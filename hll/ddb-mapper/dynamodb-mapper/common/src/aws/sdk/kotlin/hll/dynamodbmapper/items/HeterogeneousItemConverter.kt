/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.buildItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public class HeterogeneousItemConverter<T>(
    public val typeMapper: (T) -> String,
    public val typeAttribute: String,
    public val subConverters: Map<String, ItemConverter<T>>,
) : ItemConverter<T> {
    override fun fromItem(item: Item): T {
        val attr = item[typeAttribute] ?: error("Missing $typeAttribute")
        val typeValue = attr.asSOrNull() ?: error("No string value for $attr")
        val converter = subConverters[typeValue] ?: error("No converter for $typeValue")
        return converter.fromItem(item)
    }

    override fun toItem(obj: T, onlyAttributes: Set<String>?): Item {
        val typeValue = typeMapper(obj)
        val converter = subConverters[typeValue] ?: error("No converter for $typeValue")

        return buildItem {
            if (onlyAttributes?.contains(typeAttribute) != false) {
                put(typeAttribute, AttributeValue.S(typeValue))
            }

            putAll(converter.toItem(obj, onlyAttributes))
        }
    }
}
