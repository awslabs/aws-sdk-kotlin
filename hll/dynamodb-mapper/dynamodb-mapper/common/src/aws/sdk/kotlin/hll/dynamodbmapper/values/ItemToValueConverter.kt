/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public object ItemToValueConverter : ValueConverter<Item> { // Converter<Item, AttributeValue>
    override fun convertFrom(to: AttributeValue): Item = to.asM().toItem()
    override fun convertTo(from: Item): AttributeValue = AttributeValue.M(from)
}
