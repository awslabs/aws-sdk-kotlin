/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.json.RestJsonErrorMiddleware
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.JsonSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SerdeSubject
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestJson1 : AwsHttpBindingProtocolGenerator() {

    override val protocol: ShapeId = RestJson1Trait.ID
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val restJsonMiddleware = listOf(
            RestJsonErrorMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return middleware + restJsonMiddleware
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/json")

    override fun getSerdeDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        members: List<MemberShape>,
        subject: SerdeSubject,
        writer: KotlinWriter
    ): SerdeDescriptorGenerator = JsonSerdeDescriptorGenerator(ctx.toRenderingContext(this, objectShape, writer), members)
}
