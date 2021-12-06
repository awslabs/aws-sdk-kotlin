/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlSerdeDescriptorGenerator
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
open class RestXml : AwsHttpBindingProtocolGenerator() {

    override val protocol: ShapeId = RestXmlTrait.ID
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    // See https://awslabs.github.io/smithy/1.0/spec/aws/aws-restxml-protocol.html#content-type
    override fun getProtocolHttpBindingResolver(model: Model, serviceShape: ServiceShape): HttpBindingResolver =
        HttpTraitResolver(model, serviceShape, "application/xml")

    private fun renderSerializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        // order is important due to attributes
        val sortedMembers = sortMembersForSerialization(members)

        // render the serde descriptors
        RestXmlSerdeDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), sortedMembers).render()
        if (shape.isUnionShape) {
            SerializeUnionGenerator(ctx, sortedMembers, writer, defaultTimestampFormat).render()
        } else {
            SerializeStructGenerator(ctx, sortedMembers, writer, defaultTimestampFormat).render()
        }
    }

    override fun renderSerializeOperationBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter
    ) {
        val resolver = getProtocolHttpBindingResolver(ctx.model, ctx.service)
        val requestBindings = resolver.requestBindings(op)
        val documentMembers = requestBindings.filterDocumentBoundMembers()
        val shape = ctx.model.expectShape(op.input.get())

        writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlSerializer)
        writer.write("val serializer = #T()", RuntimeTypes.Serde.SerdeXml.XmlSerializer)

        val httpPayload = requestBindings.firstOrNull { it.location == HttpBinding.Location.PAYLOAD }
        if (httpPayload != null) {
            // explicitly bound member
            val member = httpPayload.member
            if (member.hasTrait<XmlNameTrait>()) {
                // can't delegate since the member has it's own trait(s), have to generate one inline
                renderSerializeExplicitBoundStructure(ctx, member, writer)
            } else {
                // re-use the document serializer
                val memberSymbol = ctx.symbolProvider.toSymbol(member)
                writer.write("input.${httpPayload.member.defaultName()}?.let { #L(serializer, it) }", memberSymbol.documentSerializerName())
            }
        } else {
            renderSerializerBody(ctx, shape, documentMembers, writer)
        }

        writer.write("return serializer.toByteArray()")
    }

    private fun renderSerializeExplicitBoundStructure(
        ctx: ProtocolGenerator.GenerationContext,
        boundMember: MemberShape,
        writer: KotlinWriter
    ) {
        val targetShape = ctx.model.expectShape<StructureShape>(boundMember.target)
        val copyWithMemberTraits = targetShape
            .toBuilder()
            .removeTrait(XmlNameTrait.ID)
            .addTrait(boundMember.expectTrait<XmlNameTrait>())
            .build()

        // re-bind and shadow local variable input such that the generate serializer body is referencing
        // the correct type (the operation `input` being shadowed isn't used)
        writer.write("val input = requireNotNull(input.${boundMember.defaultName()})")
        renderSerializerBody(ctx, copyWithMemberTraits, targetShape.members().toList(), writer)
    }

    private fun renderDeserializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        RestXmlSerdeDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members).render()
        if (shape.isUnionShape) {
            val name = ctx.symbolProvider.toSymbol(shape).name
            DeserializeUnionGenerator(ctx, name, members, writer, defaultTimestampFormat).render()
        } else {
            DeserializeStructGenerator(ctx, members, writer, defaultTimestampFormat).render()
        }
    }

    override fun renderSerializeDocumentBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        writer: KotlinWriter
    ) {
        renderSerializerBody(ctx, shape, shape.members().toList(), writer)
    }

    override fun renderDeserializeOperationBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter
    ) {
        writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        writer.write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        val resolver = getProtocolHttpBindingResolver(ctx.model, ctx.service)
        val responseBindings = resolver.responseBindings(op)
        val documentMembers = responseBindings.filterDocumentBoundMembers()

        val shape = ctx.model.expectShape(op.output.get())

        val httpPayload = responseBindings.firstOrNull { it.location == HttpBinding.Location.PAYLOAD }
        if (httpPayload != null) {
            // explicitly bound member
            val member = httpPayload.member
            if (member.hasTrait<XmlNameTrait>()) {
                // can't delegate, have to generate a dedicated deserializer inline
                renderDeserializeExplicitBoundStructure(ctx, member, writer)
            } else {
                // we can re-use the document deserializer
                val memberSymbol = ctx.symbolProvider.toSymbol(member)
                writer.write("builder.${member.defaultName()} = #L(deserializer)", memberSymbol.documentDeserializerName())
            }
        } else {
            renderDeserializerBody(ctx, shape, documentMembers, writer)
        }
    }

    private fun renderDeserializeExplicitBoundStructure(
        ctx: ProtocolGenerator.GenerationContext,
        boundMember: MemberShape,
        writer: KotlinWriter
    ) {
        val memberSymbol = ctx.symbolProvider.toSymbol(boundMember)
        writer.addImport(memberSymbol)

        val targetShape = ctx.model.expectShape<StructureShape>(boundMember.target)

        val copyWithMemberTraits = targetShape.toBuilder()
            .removeTrait(XmlNameTrait.ID)
            .addTrait(boundMember.expectTrait<XmlNameTrait>())
            .build()

        // cheat and generate a local lambda variable whose body matches that of a document serializer for the member
        // type BUT with the traits of the member. This allows the `builder` variable to have the correct scope
        // in two different contexts
        val boundMemberName = boundMember.capitalizedDefaultName()
        val deserializeLambdaIdent = "deserialize$boundMemberName"
        writer.withBlock("val $deserializeLambdaIdent = {", "}") {
            write("val builder = #T.Builder()", memberSymbol)
            renderDeserializerBody(ctx, copyWithMemberTraits, targetShape.members().toList(), writer)
            write("builder.build()")
        }

        // invoke the inline lambda
        writer.write("builder.${boundMember.defaultName()} = $deserializeLambdaIdent()")
    }

    override fun renderDeserializeDocumentBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        writer: KotlinWriter
    ) {
        renderDeserializerBody(ctx, shape, shape.members().toList(), writer)
    }

    override fun renderDeserializeException(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        writer: KotlinWriter
    ) {
        val resolver = getProtocolHttpBindingResolver(ctx.model, ctx.service)
        val responseBindings = resolver.responseBindings(shape)
        val documentMembers = responseBindings.filterDocumentBoundMembers()
        writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        writer.write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        renderDeserializerBody(ctx, shape, documentMembers, writer)
    }

    private fun sortMembersForSerialization(
        members: List<MemberShape>
    ): List<MemberShape> {
        val attributes = members.filter { it.hasTrait<XmlAttributeTrait>() }.sortedBy { it.memberName }
        val elements = members.filterNot { it.hasTrait<XmlAttributeTrait>() }.sortedBy { it.memberName }

        // XML attributes MUST be serialized immediately following calls to `startTag` before
        // any nested content is serialized
        return attributes + elements
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
