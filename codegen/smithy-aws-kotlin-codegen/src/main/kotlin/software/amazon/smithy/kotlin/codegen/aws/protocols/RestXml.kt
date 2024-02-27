/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kotlin.codegen.aws.protocols

import software.amazon.smithy.aws.traits.protocols.RestXmlTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.aws.protocols.xml.RestXmlSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.TimestampFormatTrait
import software.amazon.smithy.model.traits.XmlNameTrait
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

    // See: https://github.com/awslabs/aws-sdk-kotlin/issues/1050
    override fun renderContentTypeHeader(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
        resolver: HttpBindingResolver,
    ) {
        if (op.payloadIsUnionShape(ctx.model)) {
            writer.write("builder.headers.setMissing(\"Content-Type\", #S)", resolver.determineRequestContentType(op))
        } else {
            super.renderContentTypeHeader(ctx, op, writer, resolver)
        }
    }

    override fun structuredDataParser(ctx: ProtocolGenerator.GenerationContext): StructuredDataParserGenerator =
        RestXmlParserGenerator(this)

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
    protocolGenerator: RestXml,
) : XmlParserGenerator(protocolGenerator.defaultTimestampFormat) {

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
            namespace = ctx.settings.pkg.serde
            definitionFile = "${memberSymbol.name}PayloadDeserializer.kt"
            renderBy = { writer ->
                addNestedDocumentDeserializers(ctx, targetShape, writer)
                writer.dokka("Payload deserializer for ${memberSymbol.name} with a different XML name trait (${xmlNameTrait.value})")
                writer.withBlock("internal fun $name(payload: ByteArray): #T {", "}", memberSymbol) {
                    writer.write("val root = #T(payload)", RuntimeTypes.Serde.SerdeXml.xmlRootTagReader)
                    val serdeCtx = SerdeCtx("root")
                    write("val builder = #T.Builder()", memberSymbol)
                    renderDeserializerBody(ctx, serdeCtx, copyWithMemberTraits, targetShape.members().toList(), writer)
                    write("return builder.build()")
                }
            }
        }
    }

    override fun unwrapOperationError(
        ctx: ProtocolGenerator.GenerationContext,
        serdeCtx: SerdeCtx,
        errorShape: StructureShape,
        writer: KotlinWriter,
    ): SerdeCtx {
        val unwrapFn = when (ctx.service.getTrait<RestXmlTrait>()?.isNoErrorWrapping == true) {
            true -> RestXmlErrors.unwrappedErrorResponseDeserializer(ctx)
            false -> RestXmlErrors.wrappedErrorResponseDeserializer(ctx)
        }
        writer.write("val errReader = #T(${serdeCtx.tagReader})", unwrapFn)
        return SerdeCtx("errReader")
    }
}

object RestXmlErrors {

    /**
     * Error deserializer for a wrapped error response
     *
     * ```
     * <ErrorResponse>
     *     <Error>
     *         <-- DATA -->>
     *     </Error>
     * </ErrorResponse>
     * ```
     *
     * See https://smithy.io/2.0/aws/protocols/aws-restxml-protocol.html#error-response-serialization
     */
    fun wrappedErrorResponseDeserializer(ctx: ProtocolGenerator.GenerationContext): Symbol = buildSymbol {
        name = "unwrapWrappedXmlErrorResponse"
        namespace = ctx.settings.pkg.serde
        definitionFile = "XmlErrorUtils.kt"
        renderBy = { writer ->
            writer.dokka("Handle [wrapped](https://smithy.io/2.0/aws/protocols/aws-restxml-protocol.html#error-response-serialization) error responses")
            writer.withBlock(
                "internal fun $name(root: #1T): #1T {",
                "}",
                RuntimeTypes.Serde.SerdeXml.XmlTagReader,
            ) {
                withBlock(
                    "if (root.tagName != #S) {",
                    "}",
                    "ErrorResponse",
                ) {
                    write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "invalid root, expected <ErrorResponse>; found `\${root.tag}`")
                }

                write("val errTag = root.nextTag()")
                withBlock(
                    "if (errTag == null || errTag.tagName != #S) {",
                    "}",
                    "Error",
                ) {
                    write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "invalid error, expected <Error>; found `\${errTag?.tag}`")
                }

                write("return errTag")
            }
        }
    }

    /**
     * Error deserializer for an unwrapped error response
     *
     * ```
     * <Error>
     *    <-- DATA -->>
     * </Error>
     * ```
     *
     * See https://smithy.io/2.0/aws/protocols/aws-restxml-protocol.html#error-response-serialization
     */
    fun unwrappedErrorResponseDeserializer(ctx: ProtocolGenerator.GenerationContext): Symbol = buildSymbol {
        name = "unwrapXmlErrorResponse"
        namespace = ctx.settings.pkg.serde
        definitionFile = "XmlErrorUtils.kt"
        renderBy = { writer ->
            writer.dokka("Handle [unwrapped](https://smithy.io/2.0/aws/protocols/aws-restxml-protocol.html#error-response-serialization) error responses (restXml.noErrorWrapping == true)")
            writer.withBlock(
                "internal fun $name(root: #1T): #1T {",
                "}",
                RuntimeTypes.Serde.SerdeXml.XmlTagReader,
            ) {
                withBlock(
                    "if (root.tagName != #S) {",
                    "}",
                    "Error",
                ) {
                    write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "invalid error, expected <Error>; found `\${root.tag}`")
                }

                write("return root")
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

    // FIXME
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
            namespace = ctx.settings.pkg.serde
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
