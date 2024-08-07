/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [String] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class StringConverter : ValueConverter<String> {
    public companion object {
        /**
         * The default instance of [StringConverter]
         */
        public val Default: StringConverter = StringConverter()
    }

    override fun fromAv(attr: AttributeValue): String = attr.asS()
    override fun toAv(value: String): AttributeValue = AttributeValue.S(value)
}
