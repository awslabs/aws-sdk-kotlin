/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.json.JsonSerdeFieldGenerator
import aws.sdk.kotlin.codegen.protocols.json.RestJsonErrorMiddleware
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
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

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val restJsonMiddleware = listOf(
            RestJsonErrorMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return middleware + restJsonMiddleware
    }

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/json")

    override fun generateSdkFieldDescriptor(
        ctx: ProtocolGenerator.GenerationContext,
        memberShape: MemberShape,
        writer: KotlinWriter,
        memberTargetShape: Shape?,
        namePostfix: String
    ) = JsonSerdeFieldGenerator.generateSdkFieldDescriptor(ctx, memberShape, writer, memberTargetShape, namePostfix)

    override fun generateSdkObjectDescriptorTraits(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        writer: KotlinWriter
    ) = JsonSerdeFieldGenerator.generateSdkObjectDescriptorTraits(ctx, objectShape, writer)

    override val protocol: ShapeId = RestJson1Trait.ID
}
