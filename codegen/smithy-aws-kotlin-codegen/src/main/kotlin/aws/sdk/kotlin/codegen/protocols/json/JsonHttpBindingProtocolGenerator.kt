/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.json

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.filterDocumentBoundMembers
import software.amazon.smithy.kotlin.codegen.rendering.protocol.toRenderingContext
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * Abstract base class that all protocols using JSON as a document format can inherit from
 */
abstract class JsonHttpBindingProtocolGenerator : AwsHttpBindingProtocolGenerator() {

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS

    private fun renderSerializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        // render the serde descriptors
        JsonSerdeDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members).render()
        if (shape.isUnionShape) {
            SerializeUnionGenerator(ctx, members, writer, defaultTimestampFormat).render()
        } else {
            SerializeStructGenerator(ctx, members, writer, defaultTimestampFormat).render()
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

        // import and instantiate a serializer
        writer.addImport(RuntimeTypes.Serde.SerdeJson.JsonSerializer)
        writer.write("val serializer = #T()", RuntimeTypes.Serde.SerdeJson.JsonSerializer)

        // restJson protocol supports the httpPayload trait
        val httpPayload = requestBindings.firstOrNull { it.location == HttpBinding.Location.PAYLOAD }
        if (httpPayload != null) {
            // explicitly bound member, delegate to the document serializer
            val memberSymbol = ctx.symbolProvider.toSymbol(httpPayload.member)
            writer.write("input.${httpPayload.member.defaultName()}?.let { #L(serializer, it) }", memberSymbol.documentSerializerName())
        } else {
            renderSerializerBody(ctx, shape, documentMembers, writer)
        }

        writer.write("return serializer.toByteArray()")
    }

    private fun renderDeserializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        JsonSerdeDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members).render()
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
        writer.addImport(RuntimeTypes.Serde.SerdeJson.JsonDeserializer)
        writer.write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeJson.JsonDeserializer)
        val resolver = getProtocolHttpBindingResolver(ctx.model, ctx.service)
        val responseBindings = resolver.responseBindings(op)
        val documentMembers = responseBindings.filterDocumentBoundMembers()

        val shape = ctx.model.expectShape(op.output.get())

        val httpPayload = responseBindings.firstOrNull { it.location == HttpBinding.Location.PAYLOAD }
        if (httpPayload != null) {
            // explicitly bound member, delegate to the document deserializer
            val memberSymbol = ctx.symbolProvider.toSymbol(httpPayload.member)
            writer.write("builder.${httpPayload.member.defaultName()} = #L(deserializer)", memberSymbol.documentDeserializerName())
        } else {
            renderDeserializerBody(ctx, shape, documentMembers, writer)
        }
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
        writer.addImport(RuntimeTypes.Serde.SerdeJson.JsonDeserializer)
        writer.write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeJson.JsonDeserializer)
        renderDeserializerBody(ctx, shape, documentMembers, writer)
    }
}
