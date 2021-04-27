/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonHttpBindingResolver
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonModeledExceptionsMiddleware
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonProtocolMiddleware
import aws.sdk.kotlin.codegen.protocols.json.JsonSerdeFieldGenerator
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Handles generating the aws.protocols#awsJson1_0 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class AwsJson1_0 : AwsHttpBindingProtocolGenerator() {

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val httpMiddleware = super.getDefaultHttpMiddleware(ctx)
        val awsJsonMiddleware = listOf(
            AwsJsonProtocolMiddleware("1.0"),
            AwsJsonModeledExceptionsMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return httpMiddleware + awsJsonMiddleware
    }

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsJsonHttpBindingResolver(ctx, "application/x-amz-json-1.0")

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

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

    override val protocol: ShapeId = AwsJson1_0Trait.ID
}
