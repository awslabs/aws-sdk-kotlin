/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of [CharArray] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class CharArraySetConverter : ValueConverter<Set<CharArray>> {
    public companion object {
        /**
         * The default instance of [CharArraySetConverter]
         */
        public val Default: CharArraySetConverter = CharArraySetConverter()
    }

    override fun fromAv(attr: AttributeValue): Set<CharArray> = attr.asSs().map(String::toCharArray).toSet()
    override fun toAv(value: Set<CharArray>): AttributeValue = AttributeValue.Ss(value.map(::String).toList())
}
