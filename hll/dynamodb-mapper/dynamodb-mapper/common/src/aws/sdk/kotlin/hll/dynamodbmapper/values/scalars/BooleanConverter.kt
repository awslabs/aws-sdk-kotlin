/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between [Boolean] and
 * [DynamoDB `BOOL` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Boolean)
 */
public class BooleanConverter : ValueConverter<Boolean> {
    public companion object {
        /**
         * The default instance of [BooleanConverter]
         */
        public val Default: BooleanConverter = BooleanConverter()
    }

    override fun fromAv(attr: AttributeValue): Boolean = attr.asBool()
    override fun toAv(value: Boolean): AttributeValue = AttributeValue.Bool(value)
}
