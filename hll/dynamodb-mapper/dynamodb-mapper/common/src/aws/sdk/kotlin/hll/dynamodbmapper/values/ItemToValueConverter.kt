/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Converts between [Item] and [AttributeValue].
 * This converter is typically chained following an [ItemConverter] using the [andThenTo] extension function.
 */
@ExperimentalApi
public object ItemToValueConverter : ValueConverter<Item> {
    /**
     * Convert from [AttributeValue] to [Item]
     */
    override fun convertFrom(to: AttributeValue): Item = to.asM().toItem()

    /**
     * Convert [from] [Item] to [AttributeValue]
     */
    override fun convertTo(from: Item): AttributeValue = AttributeValue.M(from)
}
