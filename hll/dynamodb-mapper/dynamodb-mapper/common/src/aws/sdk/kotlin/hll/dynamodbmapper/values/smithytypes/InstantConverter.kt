/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds

/**
 * Provides access to [ValueConverter] types for various [Instant] representations
 */
public object InstantConverter {
    /**
     * The default converter for [Instant] values, which is an instance of [EpochS]
     */
    public val Default: ValueConverter<Instant> = EpochS.Default

    /**
     * Converts between [Instant] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * containing the number of milliseconds since the Unix epoch
     */
    public class EpochMs : ValueConverter<Instant> {
        public companion object {
            /**
             * The default instance of [EpochMs]
             */
            public val Default: EpochMs = EpochMs()
        }

        override fun fromAv(attr: AttributeValue): Instant = Instant.fromEpochMilliseconds(attr.asN().toLong())
        override fun toAv(value: Instant): AttributeValue = AttributeValue.N(value.epochMilliseconds.toString())
    }

    /**
     * Converts between [Instant] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * containing the number of seconds since the Unix epoch
     */
    public class EpochS : ValueConverter<Instant> {
        public companion object {
            /**
             * The default instance of [EpochS]
             */
            public val Default: EpochS = EpochS()
        }

        override fun fromAv(attr: AttributeValue): Instant = Instant.fromEpochSeconds(attr.asN().toLong())
        override fun toAv(value: Instant): AttributeValue = AttributeValue.N(value.epochSeconds.toString())
    }

    /**
     * Converts between [Instant] and
     * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
     * containing a formatted ISO 8601 representation
     */
    public class Iso8601 : ValueConverter<Instant> {
        public companion object {
            /**
             * The default instance of [Iso8601]
             */
            public val Default: Iso8601 = Iso8601()
        }

        override fun fromAv(attr: AttributeValue): Instant = Instant.fromIso8601(attr.asS())
        override fun toAv(value: Instant): AttributeValue =
            AttributeValue.S(value.format(TimestampFormat.ISO_8601_FULL))
    }
}
