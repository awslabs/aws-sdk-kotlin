/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.awsjson

import aws.sdk.kotlin.codegen.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.*
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
            AwsJsonProtocolMiddleware("1.1"),
            AwsJsonModeledExceptionsMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return httpMiddleware + awsJsonFeatures
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsJsonHttpBindingResolver(ctx, "application/x-amz-json-1.1")

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
}
