/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Char] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class CharConverter : ValueConverter<Char> {
    public companion object {
        /**
         * The default instance of [CharConverter]
         */
        public val Default: CharConverter = CharConverter()
    }

    override fun fromAv(attr: AttributeValue): Char = attr.asS().single()
    override fun toAv(value: Char): AttributeValue = AttributeValue.S(value.toString())
}
