/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlSerdeDescriptorGenerator
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
        HttpTraitResolver(model, serviceShape, ProtocolContentTypes.consistent("application/xml"))

    override fun structuredDataParser(ctx: ProtocolGenerator.GenerationContext): StructuredDataParserGenerator =
        RestXmlParserGenerator(this, defaultTimestampFormat)

    override fun structuredDataSerializer(ctx: ProtocolGenerator.GenerationContext): StructuredDataSerializerGenerator =
        RestXmlSerializerGenerator(this, defaultTimestampFormat)

    override fun renderDeserializeErrorDetails(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
    ) {
        writer.write("""checkNotNull(payload){ "unable to parse error from empty response" }""")
        writer.write("#T(payload)", RuntimeTypes.AwsXmlProtocols.parseRestXmlErrorResponse)
    }
}

class RestXmlParserGenerator(
    private val protocolGenerator: RestXml,
    defaultTimestampFormat: TimestampFormatTrait.Format,
) : XmlParserGenerator(protocolGenerator, defaultTimestampFormat) {

    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): XmlSerdeDescriptorGenerator = RestXmlSerdeDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)

    override fun payloadDeserializer(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: Collection<MemberShape>?,
    ): Symbol = when {
        // can't delegate, have to generate a dedicated deserializer because the member xml name is different
        // from the name of the target shape
        isXmlNamedMemberShape(shape) -> explicitBoundStructureDeserializer(ctx, shape)
        else -> super.payloadDeserializer(ctx, shape, members)
    }

    private fun explicitBoundStructureDeserializer(
        ctx: ProtocolGenerator.GenerationContext,
        boundMember: MemberShape,
    ): Symbol {
        val memberSymbol = ctx.symbolProvider.toSymbol(boundMember)
        val targetShape = ctx.model.expectShape<StructureShape>(boundMember.target)

        val xmlNameTrait = boundMember.expectTrait<XmlNameTrait>()
        val copyWithMemberTraits = targetShape.toBuilder()
            .removeTrait(XmlNameTrait.ID)
            .addTrait(xmlNameTrait)
            .build()

        return buildSymbol {
            val xmlName = xmlNameTrait.value.replaceFirstChar(Char::uppercase)
            name = "deserialize${memberSymbol.name}PayloadWithXmlName$xmlName"
            namespace = ctx.settings.pkg.subpackage("transform")
            definitionFile = "${memberSymbol.name}PayloadDeserializer.kt"
            renderBy = { writer ->
                addNestedDocumentDeserializers(ctx, targetShape, writer)
                writer.dokka("Payload deserializer for ${memberSymbol.name} with a different XML name trait (${xmlNameTrait.value})")
                writer.withBlock("internal fun $name(payload: ByteArray): #T {", "}", memberSymbol) {
                    write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
                    write("val builder = #T.Builder()", memberSymbol)
                    renderDeserializerBody(ctx, copyWithMemberTraits, targetShape.members().toList(), writer)
                    write("return builder.build()")
                }
            }
        }
    }
}

class RestXmlSerializerGenerator(
    private val protocolGenerator: RestXml,
    defaultTimestampFormat: TimestampFormatTrait.Format,
) : XmlSerializerGenerator(protocolGenerator, defaultTimestampFormat) {

    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): XmlSerdeDescriptorGenerator = RestXmlSerdeDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)

    override fun payloadSerializer(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: Collection<MemberShape>?,
    ): Symbol = when {
        // can't delegate, have to generate a dedicated serializer because the member xml name is different
        // from the name of the target shape
        isXmlNamedMemberShape(shape) -> explicitBoundStructureSerializer(ctx, shape)
        else -> super.payloadSerializer(ctx, shape, members)
    }

    private fun explicitBoundStructureSerializer(
        ctx: ProtocolGenerator.GenerationContext,
        boundMember: MemberShape,
    ): Symbol {
        val memberSymbol = ctx.symbolProvider.toSymbol(boundMember)
        val targetShape = ctx.model.expectShape<StructureShape>(boundMember.target)

        val xmlNameTrait = boundMember.expectTrait<XmlNameTrait>()
        val copyWithMemberTraits = targetShape.toBuilder()
            .removeTrait(XmlNameTrait.ID)
            .addTrait(xmlNameTrait)
            .build()

        // we need a unique function specific to this XmlName
        return buildSymbol {
            val xmlName = xmlNameTrait.value.replaceFirstChar(Char::uppercase)
            name = "serialize${memberSymbol.name}PayloadWithXmlName$xmlName"
            namespace = ctx.settings.pkg.subpackage("transform")
            // TODO - it would be nice to just inline this into the operation file as a private function instead
            //  since that is the only place it should be accessed
            definitionFile = "${memberSymbol.name}PayloadSerializer.kt"
            renderBy = { writer ->
                addNestedDocumentSerializers(ctx, targetShape, writer)
                writer.dokka("Payload serializer for ${memberSymbol.name} with a different XML name trait (${xmlNameTrait.value})")
                writer.withBlock("internal fun $name(input: #T): ByteArray {", "}", memberSymbol) {
                    write("val serializer = #T()", RuntimeTypes.Serde.SerdeXml.XmlSerializer)
                    renderSerializerBody(ctx, copyWithMemberTraits, targetShape.members().toList(), writer)
                    write("return serializer.toByteArray()")
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
private fun isXmlNamedMemberShape(shape: Shape): Boolean {
    contract {
        returns(true) implies (shape is MemberShape)
    }
    return shape.hasTrait<XmlNameTrait>() && shape is MemberShape
}
