/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.net.url.Url

/**
 * Converts between [Url] and
 * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
 */
public class UrlConverter : ValueConverter<Url> {
    public companion object {
        /**
         * The default instance of [UrlConverter]
         */
        public val Default: UrlConverter = UrlConverter()
    }

    override fun fromAv(attr: AttributeValue): Url = Url.parse(attr.asS())
    override fun toAv(value: Url): AttributeValue = AttributeValue.S(value.toString())
}
