/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.smithytypes

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.LongConverter
import aws.sdk.kotlin.hll.dynamodbmapper.values.scalars.StringConverter
import aws.sdk.kotlin.hll.mapping.core.converters.Converter
import aws.sdk.kotlin.hll.mapping.core.converters.andThenTo
import aws.smithy.kotlin.runtime.ExperimentalApi
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds

/**
 * Provides access to [ValueConverter] types for various [Instant] representations
 */
@ExperimentalApi
public object InstantConverter {
    /**
     * Converts between [Instant] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * containing the number of milliseconds since the Unix epoch
     */
    public val EpochMs: ValueConverter<Instant> =
        Converter(Instant::epochMilliseconds, Instant::fromEpochMilliseconds).andThenTo(LongConverter)

    /**
     * Converts between [Instant] and
     * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
     * containing the number of seconds since the Unix epoch
     */
    public val EpochS: ValueConverter<Instant> =
        Converter(Instant::epochSeconds, Instant::fromEpochSeconds).andThenTo(LongConverter)

    /**
     * Converts between [Instant] and
     * [DynamoDB `S` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.String)
     * containing a formatted ISO 8601 representation
     */
    public val Iso8601: ValueConverter<Instant> =
        Converter(
            convertTo = { from: Instant -> from.format(TimestampFormat.ISO_8601_FULL) },
            convertFrom = Instant::fromIso8601,
        ).andThenTo(StringConverter)

    /**
     * The default converter for [Instant] values, which is an instance of [EpochS]
     */
    public val Default: ValueConverter<Instant> = EpochS
}
