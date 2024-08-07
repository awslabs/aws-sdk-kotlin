/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of [Char] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class CharSetConverter : ValueConverter<Set<Char>> {
    public companion object {
        /**
         * The default instance of [CharSetConverter]
         */
        public val Default: CharSetConverter = CharSetConverter()
    }

    override fun fromAv(attr: AttributeValue): Set<Char> = attr.asSs().map(String::single).toSet()
    override fun toAv(value: Set<Char>): AttributeValue = AttributeValue.Ss(value.map(Char::toString).toList())
}
