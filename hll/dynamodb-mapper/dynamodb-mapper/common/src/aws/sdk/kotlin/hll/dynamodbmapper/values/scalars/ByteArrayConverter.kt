/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [ByteArray] and
 * [DynamoDB `B` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Binary)
 */
public class ByteArrayConverter : ValueConverter<ByteArray> {
    public companion object {
        /**
         * The default instance of [ByteArrayConverter]
         */
        public val Default: ByteArrayConverter = ByteArrayConverter()
    }

    override fun fromAttributeValue(attr: AttributeValue): ByteArray = attr.asB()
    override fun toAttributeValue(value: ByteArray): AttributeValue = AttributeValue.B(value)
}
