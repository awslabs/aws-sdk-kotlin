/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.TimestampFormat
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds

public object InstantConverter {
    public val Default: ValueConverter<Instant> = EpochMs

    public object EpochMs : ValueConverter<Instant> {
        override fun fromAv(attr: AttributeValue): Instant =
            Instant.fromEpochMilliseconds(attr.asN().toLong())

        override fun toAv(value: Instant): AttributeValue =
            AttributeValue.N(value.epochMilliseconds.toString())
    }

    public object EpochS : ValueConverter<Instant> {
        override fun fromAv(attr: AttributeValue): Instant =
            Instant.fromEpochSeconds(attr.asN().toLong())

        override fun toAv(value: Instant): AttributeValue =
            AttributeValue.N(value.epochSeconds.toString())
    }

    public object Iso8601 : ValueConverter<Instant> {
        override fun fromAv(attr: AttributeValue): Instant =
            Instant.fromIso8601(attr.asS())

        override fun toAv(value: Instant): AttributeValue =
            AttributeValue.S(value.format(TimestampFormat.ISO_8601))
    }
}
