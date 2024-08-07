/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [CharArray] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class CharArrayConverter : ValueConverter<CharArray> {
    public companion object {
        /**
         * The default instance of [CharArrayConverter]
         */
        public val Default: CharArrayConverter = CharArrayConverter()
    }

    override fun fromAv(attr: AttributeValue): CharArray = attr.asS().toCharArray()
    override fun toAv(value: CharArray): AttributeValue = AttributeValue.S(String(value))
}
