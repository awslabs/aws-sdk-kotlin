/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.restjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.awsjson.JsonSerdeFieldGenerator
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.*
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.TimestampFormatTrait

// The default Http Binding resolver is used for both white-label smithy-kotlin tests
// and as the restJson1 binding resolver.  If/when AWS-specific logic needs to
// be added to the resolver which is not "white label" in character, these types
// should be broken into two: one purely scoped for white-label SDK testing and one
// for restJson1 support.
typealias RestJsonHttpBindingResolver = HttpTraitResolver

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
        RestJsonHttpBindingResolver(ctx, "application/json")

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
