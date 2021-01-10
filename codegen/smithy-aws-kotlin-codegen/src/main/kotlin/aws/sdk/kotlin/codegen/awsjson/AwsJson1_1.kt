/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.awsjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.JsonSerdeFeature
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait
import software.amazon.smithy.kotlin.codegen.hasIdempotentTokenMember
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Handles generating the aws.protocols#awsJson1_1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class AwsJson1_1 : AwsHttpBindingProtocolGenerator() {
    override val protocol: ShapeId = AwsJson1_1Trait.ID
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val parentFeatures = super.getHttpFeatures(ctx)
        val awsJsonFeatures = listOf(
            JsonSerdeFeature(ctx.service.hasIdempotentTokenMember(ctx.model)),
            AwsJsonTargetHeaderFeature("1.1"),
            AwsJsonModeledExceptionsFeature(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return parentFeatures + awsJsonFeatures
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsJsonHttpBindingResolver(ctx, "application/x-amz-json-1.1")
}
