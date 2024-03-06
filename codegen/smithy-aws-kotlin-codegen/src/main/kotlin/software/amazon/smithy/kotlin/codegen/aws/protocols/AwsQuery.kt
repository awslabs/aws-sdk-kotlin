/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.kotlin.codegen.aws.protocols

import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.AbstractQueryFormUrlSerializerGenerator
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.aws.protocols.core.QueryHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.aws.protocols.formurl.QuerySerdeFormUrlDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
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
        writer: KotlinWriter,
    ) {
        writer.write("""checkNotNull(payload){ "unable to parse error from empty response" }""")
        writer.write("#T(payload)", RuntimeTypes.AwsXmlProtocols.parseRestXmlErrorResponse)
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

private class AwsQuerySerializerGenerator(
    private val protocolGenerator: AwsQuery,
) : AbstractQueryFormUrlSerializerGenerator(protocolGenerator, protocolGenerator.defaultTimestampFormat) {
    override fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): FormUrlSerdeDescriptorGenerator = AwsQuerySerdeFormUrlDescriptorGenerator(ctx.toRenderingContext(protocolGenerator, shape, writer), members)
}

private class AwsQueryXmlParserGenerator(
    protocolGenerator: AwsQuery,
) : XmlParserGenerator(protocolGenerator.defaultTimestampFormat) {

    /**
     * Unwraps the response body as specified by
     * https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#response-serialization so that the
     * deserializer is in the correct state.
     *
     * ```
     * <SomeOperationResponse>
     *     <SomeOperationResult>
     *          <-- SAME AS REST XML -->
     *     </SomeOperationResult>
     *</SomeOperationResponse>
     * ```
     */
    override fun unwrapOperationBody(
        ctx: ProtocolGenerator.GenerationContext,
        serdeCtx: SerdeCtx,
        op: OperationShape,
        writer: KotlinWriter,
    ): SerdeCtx {
        val operationName = op.id.getName(ctx.service)

        val unwrapAwsQueryOperation = buildSymbol {
            name = "unwrapAwsQueryResponse"
            namespace = ctx.settings.pkg.serde
            definitionFile = "AwsQueryUtil.kt"
            renderBy = { writer ->

                writer.withBlock(
                    "internal fun $name(root: #1T, operationName: #2T): #1T {",
                    "}",
                    RuntimeTypes.Serde.SerdeXml.XmlTagReader,
                    KotlinTypes.String,
                ) {
                    write("val responseWrapperName = \"\${operationName}Response\"")
                    write("val resultWrapperName = \"\${operationName}Result\"")
                    withBlock(
                        "if (root.tagName != responseWrapperName) {",
                        "}",
                    ) {
                        write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "invalid root, expected \$responseWrapperName; found `\${root.tag}`")
                    }

                    write("val resultTag = ${serdeCtx.tagReader}.nextTag()")
                    withBlock(
                        "if (resultTag == null || resultTag.tagName != resultWrapperName) {",
                        "}",
                    ) {
                        write("throw #T(#S)", RuntimeTypes.Serde.DeserializationException, "invalid result, expected \$resultWrapperName; found `\${resultTag?.tag}`")
                    }

                    write("return resultTag")
                }
            }
        }

        writer.write("val unwrapped = #T(#L, #S)", unwrapAwsQueryOperation, serdeCtx.tagReader, operationName)

        return SerdeCtx("unwrapped")
    }

    override fun unwrapOperationError(
        ctx: ProtocolGenerator.GenerationContext,
        serdeCtx: SerdeCtx,
        errorShape: StructureShape,
        writer: KotlinWriter,
    ): SerdeCtx {
        writer.write("val errReader = #T(${serdeCtx.tagReader})", RestXmlErrors.wrappedErrorResponseDeserializer(ctx))
        return SerdeCtx("errReader")
    }
}
