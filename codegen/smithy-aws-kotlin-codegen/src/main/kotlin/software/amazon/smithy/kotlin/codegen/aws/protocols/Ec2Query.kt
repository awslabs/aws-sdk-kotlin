/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.kotlin.codegen.aws.protocols

import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.AbstractQueryFormUrlSerializerGenerator
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.QueryHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.aws.protocols.formurl.QuerySerdeFormUrlDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.model.changeNameSuffix
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.traits.OperationOutput
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.toRenderingContext
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.XmlNameTrait

/**
 * Handles generating the aws.protocols#ec2Query protocol for services.
 */
class Ec2Query : QueryHttpBindingProtocolGenerator() {
    override val protocol: ShapeId = Ec2QueryTrait.ID

    override fun structuredDataSerializer(ctx: ProtocolGenerator.GenerationContext): StructuredDataSerializerGenerator =
        Ec2QuerySerializerGenerator(this)

    override fun structuredDataParser(ctx: ProtocolGenerator.GenerationContext): StructuredDataParserGenerator =
        Ec2QueryParserGenerator(this)

    override fun renderDeserializeErrorDetails(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
    ) {
        writer.write("""checkNotNull(payload){ "unable to parse error from empty response" }""")
        writer.write("#T(payload)", RuntimeTypes.AwsXmlProtocols.parseEc2QueryErrorResponse)
    }
}

private class Ec2QuerySerdeFormUrlDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
) : QuerySerdeFormUrlDescriptorGenerator(ctx, memberShapes) {
    /**
     * The serialized name for a shape. See
     * [EC2 query protocol](https://awslabs.github.io/smithy/1.0/spec/aws/aws-ec2-query-protocol.html#query-key-resolution)
     * for more information.
     */
    override val objectSerialName: String
        get() =
            objectShape.getTrait<Ec2QueryNameTrait>()?.value
                ?: objectShape.getTrait<XmlNameTrait>()?.value?.replaceFirstChar(Char::uppercaseChar)
                ?: super.objectSerialName

    override fun getMemberSerialNameOverride(member: MemberShape): String? =
        member.getTrait<Ec2QueryNameTrait>()?.value
            ?: member.getTrait<XmlNameTrait>()?.value?.replaceFirstChar(Char::uppercaseChar)
            ?: if (member.memberName.firstOrNull()?.isUpperCase() == false) {
                member.memberName.replaceFirstChar(Char::uppercaseChar)
            } else {
                null
            }

    override fun isMemberFlattened(member: MemberShape, targetShape: Shape): Boolean =
        targetShape.type == ShapeType.LIST
}

private class Ec2QuerySerdeXmlDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
) : XmlSerdeDescriptorGenerator(ctx, memberShapes) {

    override fun getObjectDescriptorTraits(): List<SdkFieldDescriptorTrait> {
        val traits = super.getObjectDescriptorTraits().toMutableList()

        if (objectShape.hasTrait<OperationOutput>()) {
            traits.removeIf { it.symbol == RuntimeTypes.Serde.SerdeXml.XmlSerialName }
            val serialName = objectShape.changeNameSuffix("Response" to "Result")
            traits.add(RuntimeTypes.Serde.SerdeXml.XmlSerialName, serialName.dq())
        }

        return traits
    }
}

private class Ec2QuerySerializerGenerator(
    private val protocolGenerator: Ec2Query,
) : AbstractQueryFormUrlSerializerGenerator(protocolGenerator, protocolGenerator.defaultTimestampFormat) {

    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): FormUrlSerdeDescriptorGenerator = Ec2QuerySerdeFormUrlDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)
}

private class Ec2QueryParserGenerator(
    private val protocolGenerator: Ec2Query,
) : XmlParserGenerator(protocolGenerator, protocolGenerator.defaultTimestampFormat) {

    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): XmlSerdeDescriptorGenerator = Ec2QuerySerdeXmlDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)
}
