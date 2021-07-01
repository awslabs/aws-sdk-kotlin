/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.core.StaticHttpBindingResolver
import aws.sdk.kotlin.codegen.protocols.formurl.GeneralQuerySerdeFormUrlDescriptorGenerator
import aws.sdk.kotlin.codegen.protocols.middleware.ModeledExceptionsMiddleware
import aws.sdk.kotlin.codegen.protocols.middleware.MutateHeadersMiddleware
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.traits.OperationOutput
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

private const val AwsQueryContentType: String = "application/x-www-form-urlencoded"

/**
 * Handles generating the aws.protocols#awsQuery protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
open class AwsQuery : AwsHttpBindingProtocolGenerator() {
    override val protocol: ShapeId = AwsQueryTrait.ID

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsQueryBindingResolver(ctx)

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val awsQueryMiddleware = listOf(
            getErrorMiddleware(ctx),
            // ensure content-type gets set
            // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#protocol-behavior
            MutateHeadersMiddleware(addMissingHeaders = mapOf("Content-Type" to AwsQueryContentType))
        )

        return middleware + awsQueryMiddleware
    }

    /**
     * Get the [ModeledExceptionsMiddleware] for this protocol. The default implementation is [AwsQueryErrorMiddleware]
     * but can be overridden by subclassing protocols.
     */
    open fun getErrorMiddleware(ctx: ProtocolGenerator.GenerationContext): ModeledExceptionsMiddleware =
        AwsQueryErrorMiddleware(ctx, getProtocolHttpBindingResolver(ctx))

    override fun renderSerializeHttpBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter
    ) {
        val input = ctx.model.expectShape<StructureShape>(op.input.get())
        if (input.members().isEmpty()) {
            // if there is no payload serialized we still need to add the literals that define the operation being
            // invoked
            // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#request-serialization
            writer.addImport(RuntimeTypes.Http.ByteArrayContent)
            val action = op.id.name
            val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
            val version = service.version
            writer.write("""val content = "Action=$action&Version=$version"""")
            writer.write("builder.body = ByteArrayContent(content.encodeToByteArray())")
        } else {
            super.renderSerializeHttpBody(ctx, op, writer)
        }
    }

    private fun renderSerializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        // render the serde descriptors
        getSerializerDescriptorGenerator(ctx, shape, members, writer).render()
        if (shape.isUnionShape) {
            SerializeUnionGenerator(ctx, members, writer, defaultTimestampFormat).render()
        } else {
            SerializeStructGenerator(ctx, members, writer, defaultTimestampFormat).render()
        }
    }

    /**
     * Gets the [AbstractSerdeDescriptorGenerator] to use for serializers. By default, this is an
     * [AwsQuerySerdeFormUrlDescriptorGenerator] but this can be overridden by subclassing protocols.
     */
    open fun getSerializerDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): AbstractSerdeDescriptorGenerator =
        AwsQuerySerdeFormUrlDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members)

    override fun renderSerializeOperationBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter
    ) {
        val resolver = getProtocolHttpBindingResolver(ctx)
        val requestBindings = resolver.requestBindings(op)
        val documentMembers = requestBindings.filterDocumentBoundMembers()

        val shape = ctx.model.expectShape(op.input.get())

        // import and instantiate a serializer
        writer.addImport(RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
        writer.write("val serializer = #T()", RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
        renderSerializerBody(ctx, shape, documentMembers, writer)
        writer.write("return serializer.toByteArray()")
    }

    private fun renderDeserializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        getDeserializerDescriptorGenerator(ctx, shape, members, writer).render()
        if (shape.isUnionShape) {
            val name = ctx.symbolProvider.toSymbol(shape).name
            DeserializeUnionGenerator(ctx, name, members, writer, defaultTimestampFormat).render()
        } else {
            DeserializeStructGenerator(ctx, members, writer, defaultTimestampFormat).render()
        }
    }

    /**
     * Gets the [AbstractSerdeDescriptorGenerator] to use for deserializers. By default, this is an
     * [AwsQuerySerdeXmlDescriptorGenerator] but this can be overridden by subclassing protocols.
     */
    open fun getDeserializerDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): AbstractSerdeDescriptorGenerator =
        AwsQuerySerdeXmlDescriptorGenerator(ctx.toRenderingContext(this, shape, writer), members)

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

        val resolver = getProtocolHttpBindingResolver(ctx)
        val responseBindings = resolver.responseBindings(op)
        val documentMembers = responseBindings.filterDocumentBoundMembers()

        val shape = ctx.model.expectShape(op.output.get())

        unwrapOperationResponseBody(op.id.name, writer)
        renderDeserializerBody(ctx, shape, documentMembers, writer)
    }

    /**
     * Unwraps the response body as specified by
     * https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#response-serialization so that the
     * deserializer is in the correct state.
     */
    open fun unwrapOperationResponseBody(
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
        val resolver = getProtocolHttpBindingResolver(ctx)
        val responseBindings = resolver.responseBindings(shape)
        val documentMembers = responseBindings.filterDocumentBoundMembers()
        writer.addImport(RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        writer.write("val deserializer = #T(payload)", RuntimeTypes.Serde.SerdeXml.XmlDeserializer)
        renderDeserializerBody(ctx, shape, documentMembers, writer)
    }
}

/**
 * An HTTP binding resolver for the awsQuery protocol
 */
class AwsQueryBindingResolver(
    context: ProtocolGenerator.GenerationContext,
) : StaticHttpBindingResolver(context, AwsQueryHttpTrait, AwsQueryContentType, TimestampFormatTrait.Format.DATE_TIME) {
    companion object {
        val AwsQueryHttpTrait: HttpTrait = HttpTrait
            .builder()
            .code(200)
            .method("POST")
            .uri(UriPattern.parse("/"))
            .build()
    }
}

private class AwsQuerySerdeFormUrlDescriptorGenerator(
    ctx: RenderingContext<Shape>,
    memberShapes: List<MemberShape>? = null,
) : GeneralQuerySerdeFormUrlDescriptorGenerator(ctx, memberShapes) {
    /**
     * The serialized name for a shape. See
     * [AWS query protocol](https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#query-key-resolution)
     * for more information.
     */
    override val serialName: String
        get() = objectShape.getTrait<XmlNameTrait>()?.value ?: super.serialName

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
            val opName = objectShape.id.name.removeSuffix("Result")
            val serialName = "${opName}Response"
            traits.add(RuntimeTypes.Serde.SerdeXml.XmlSerialName, serialName.dq())
        }

        return traits
    }
}

internal class AwsQueryErrorMiddleware(
    ctx: ProtocolGenerator.GenerationContext,
    httpBindingResolver: HttpBindingResolver
) : ModeledExceptionsMiddleware(ctx, httpBindingResolver) {
    // the restxml error middleware handles both wrapped and unwrapped responses and thus is re-usable for query errors
    override val name: String = "RestXmlError"

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        writer.addImport("RestXmlError", AwsKotlinDependency.AWS_XML_PROTOCOLS)
    }

    override fun renderRegisterErrors(writer: KotlinWriter) {
        val errors = getModeledErrors()

        errors.forEach { errShape ->
            val code = errShape.getTrait<AwsQueryErrorTrait>()?.code ?: errShape.id.name

            val symbol = ctx.symbolProvider.toSymbol(errShape)
            val deserializerName = "${symbol.name}Deserializer"
            val defaultCode = if (errShape.expectTrait<ErrorTrait>().isClientError) 400 else 500
            val httpStatusCode = when {
                errShape.hasTrait<AwsQueryErrorTrait>() -> errShape.expectTrait<AwsQueryErrorTrait>().httpResponseCode
                else -> errShape.getTrait<HttpErrorTrait>()?.code ?: defaultCode
            }
            writer.write("register(code = #S, deserializer = $deserializerName(), httpStatusCode = $httpStatusCode)", code)
        }
    }
}
