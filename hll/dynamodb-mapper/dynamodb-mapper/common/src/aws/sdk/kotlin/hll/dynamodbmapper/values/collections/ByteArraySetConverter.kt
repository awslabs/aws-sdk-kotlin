/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of [ByteArray] elements and
 * [DynamoDB `BS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class ByteArraySetConverter : ValueConverter<Set<ByteArray>> {
    public companion object {
        /**
         * The default instance of [ByteArraySetConverter]
         */
        public val Default: ByteArraySetConverter = ByteArraySetConverter()
    }

    override fun fromAttributeValue(attr: AttributeValue): Set<ByteArray> = attr.asBs().toSet()
    override fun toAttributeValue(value: Set<ByteArray>): AttributeValue = AttributeValue.Bs(value.toList())
}
