/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.core.QueryHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.formurl.QuerySerdeFormUrlDescriptorGenerator
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
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

    override fun getDeserializerDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): AbstractSerdeDescriptorGenerator =
        AwsQuerySerdeXmlDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members)

    override fun getSerializerDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): AbstractSerdeDescriptorGenerator =
        AwsQuerySerdeFormUrlDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members)

    /**
     * Unwraps the response body as specified by
     * https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#response-serialization so that the
     * deserializer is in the correct state.
     */
    override fun unwrapOperationResponseBody(
        operationName: String,
        writer: KotlinWriter
    ) {
        writer
            .addImport(
                RuntimeTypes.Serde.SdkFieldDescriptor,
                RuntimeTypes.Serde.SerdeXml.XmlSerialName,
                RuntimeTypes.Serde.SdkObjectDescriptor,
                RuntimeTypes.Serde.deserializeStruct
            )
            .write("")
            .write("val resultDescriptor = #T(SerialKind.Struct, #T(#S))", RuntimeTypes.Serde.SdkFieldDescriptor, RuntimeTypes.Serde.SerdeXml.XmlSerialName, "${operationName}Result")
            .openBlock("val wrapperDescriptor = #T.build {", "}", RuntimeTypes.Serde.SdkObjectDescriptor) {
                writer
                    .addImport(RuntimeTypes.Serde.field)
                    .write("trait(#T(#S))", RuntimeTypes.Serde.SerdeXml.XmlSerialName, "${operationName}Response")
                    .write("#T(resultDescriptor)", RuntimeTypes.Serde.field)
            }
            .write("")
            // abandon the iterator, this only occurs at the top level operational output
            .write("val wrapper = deserializer.#T(wrapperDescriptor)", RuntimeTypes.Serde.deserializeStruct)
            .openBlock("if (wrapper.findNextFieldIndex() != resultDescriptor.index) {", "}") {
                writer
                    .addImport(RuntimeTypes.Serde.DeserializationException)
                    .write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "failed to unwrap $operationName response")
            }
        writer.write("")
    }

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
