/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonHttpBindingResolver
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonModeledExceptionsMiddleware
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonProtocolMiddleware
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.protocol.toRenderingContext
import software.amazon.smithy.kotlin.codegen.rendering.serde.JsonSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SerdeSubject
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
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

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val httpMiddleware = super.getDefaultHttpMiddleware(ctx)
        val awsJsonFeatures = listOf(
            AwsJsonProtocolMiddleware(ctx.settings.service, "1.1"),
            AwsJsonModeledExceptionsMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return httpMiddleware + awsJsonFeatures
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsJsonHttpBindingResolver(ctx, "application/x-amz-json-1.1")

    override fun getSerdeDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        members: List<MemberShape>,
        subject: SerdeSubject,
        writer: KotlinWriter
    ): SerdeDescriptorGenerator = JsonSerdeDescriptorGenerator(ctx.toRenderingContext(this, objectShape, writer), members)
}
