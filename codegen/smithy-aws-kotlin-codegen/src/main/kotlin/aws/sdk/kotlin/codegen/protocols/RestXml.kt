/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlErrorMiddleware
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestXml : AwsHttpBindingProtocolGenerator() {

    override val protocol: ShapeId = RestXmlTrait.ID
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val restXmlMiddleware = listOf(
            RestXmlErrorMiddleware(ctx, getProtocolHttpBindingResolver(ctx))
        )

        return middleware + restXmlMiddleware
    }

    // See https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#content-type
    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        HttpTraitResolver(ctx, "application/xml")

    override fun sortMembersForSerialization(
        ctx: ProtocolGenerator.GenerationContext,
        members: List<MemberShape>
    ): List<MemberShape> {
        val attributes = members.filter { it.hasTrait<XmlAttributeTrait>() }.sortedBy { it.memberName }
        val elements = members.filterNot { it.hasTrait<XmlAttributeTrait>() }.sortedBy { it.memberName }

        // XML attributes MUST be serialized immediately following calls to `startTag` before
        // any nested content is serialized
        return attributes + elements
    }

    override fun getSerdeDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        members: List<MemberShape>,
        subject: SerdeSubject,
        writer: KotlinWriter
    ): SerdeDescriptorGenerator = XmlSerdeDescriptorGenerator(ctx.toRenderingContext(this, objectShape, writer), members)
}
