/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.core.AbstractQueryFormUrlSerializerGenerator
import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.core.QueryHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.formurl.QuerySerdeFormUrlDescriptorGenerator
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.model.traits.OperationOutput
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

/**
 * Handles generating the aws.protocols#awsQuery protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class AwsQuery : QueryHttpBindingProtocolGenerator() {
    override val protocol: ShapeId = AwsQueryTrait.ID

    override fun structuredDataSerializer(ctx: ProtocolGenerator.GenerationContext): StructuredDataSerializerGenerator =
        AwsQuerySerializerGenerator(this)

    override fun structuredDataParser(ctx: ProtocolGenerator.GenerationContext): StructuredDataParserGenerator =
        AwsQueryXmlParserGenerator(this)

    override fun getErrorCode(ctx: ProtocolGenerator.GenerationContext, errShapeId: ShapeId): String {
        val errShape = ctx.model.expectShape(errShapeId)
        return errShape.getTrait<AwsQueryErrorTrait>()?.code ?: errShape.id.name
    }

    override fun renderDeserializeErrorDetails(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter
    ) {
        writer.addImport(AwsRuntimeTypes.XmlProtocols.parseRestXmlErrorResponse)
        writer.write("""checkNotNull(payload){ "unable to parse error from empty response" }""")
        writer.write("#T(payload)", AwsRuntimeTypes.XmlProtocols.parseRestXmlErrorResponse)
    }
}

private class AwsQuerySerdeFormUrlDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
) : QuerySerdeFormUrlDescriptorGenerator(ctx, memberShapes) {
    /**
     * The serialized name for a shape. See
     * [AWS query protocol](https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#query-key-resolution)
     * for more information.
     */
    override val objectSerialName: String
        get() = objectShape.getTrait<XmlNameTrait>()?.value ?: super.objectSerialName

    override fun getMemberSerialNameOverride(member: MemberShape): String? = member.getTrait<XmlNameTrait>()?.value

    override fun isMemberFlattened(member: MemberShape, targetShape: Shape): Boolean =
        member.hasTrait<XmlFlattenedTrait>()
}

private class AwsQuerySerdeXmlDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null
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

private class AwsQuerySerializerGenerator(
    private val protocolGenerator: AwsQuery
) : AbstractQueryFormUrlSerializerGenerator(protocolGenerator, protocolGenerator.defaultTimestampFormat) {
    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter
    ): FormUrlSerdeDescriptorGenerator = AwsQuerySerdeFormUrlDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)
}

private class AwsQueryXmlParserGenerator(
    private val protocolGenerator: AwsQuery
) : XmlParserGenerator(protocolGenerator, protocolGenerator.defaultTimestampFormat) {

    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter
    ): XmlSerdeDescriptorGenerator = AwsQuerySerdeXmlDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)

    override fun renderDeserializeOperationBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        documentMembers: List<MemberShape>,
        writer: KotlinWriter
    ) {
        writer.write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        unwrapOperationResponseBody(op.id.name, writer)
        val shape = ctx.model.expectShape(op.output.get())
        renderDeserializerBody(ctx, shape, documentMembers, writer)
    }

    /**
     * Unwraps the response body as specified by
     * https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#response-serialization so that the
     * deserializer is in the correct state.
     */
    private fun unwrapOperationResponseBody(
        operationName: String,
        writer: KotlinWriter
    ) {
        writer.write("// begin unwrap response wrapper")
            .write("val resultDescriptor = #T(#T.Struct, #T(#S))", RuntimeTypes.Serde.SdkFieldDescriptor, RuntimeTypes.Serde.SerialKind, RuntimeTypes.Serde.SerdeXml.XmlSerialName, "${operationName}Result")
            .withBlock("val wrapperDescriptor = #T.build {", "}", RuntimeTypes.Serde.SdkObjectDescriptor) {
                write("trait(#T(#S))", RuntimeTypes.Serde.SerdeXml.XmlSerialName, "${operationName}Response")
                write("#T(resultDescriptor)", RuntimeTypes.Serde.field)
            }
            .write("")
            // abandon the iterator, this only occurs at the top level operational output
            .write("val wrapper = deserializer.#T(wrapperDescriptor)", RuntimeTypes.Serde.deserializeStruct)
            .withBlock("if (wrapper.findNextFieldIndex() != resultDescriptor.index) {", "}") {
                write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "failed to unwrap $operationName response")
            }
            .write("// end unwrap response wrapper")
            .write("")
    }
}
