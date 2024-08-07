/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of [String] elements and
 * [DynamoDB `SS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class StringSetConverter : ValueConverter<Set<String>> {
    public companion object {
        /**
         * The default instance of [StringSetConverter]
         */
        public val Default: StringSetConverter = StringSetConverter()
    }

    override fun fromAv(attr: AttributeValue): Set<String> = attr.asSs().toSet()
    override fun toAv(value: Set<String>): AttributeValue = AttributeValue.Ss(value.toList())
}
