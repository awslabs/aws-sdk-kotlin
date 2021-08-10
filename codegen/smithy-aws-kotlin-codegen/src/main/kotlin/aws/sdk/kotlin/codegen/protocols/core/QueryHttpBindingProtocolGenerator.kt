package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.protocols.middleware.ModeledExceptionsMiddleware
import aws.sdk.kotlin.codegen.protocols.middleware.MutateHeadersMiddleware
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.rendering.protocol.filterDocumentBoundMembers
import software.amazon.smithy.kotlin.codegen.rendering.serde.*
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.traits.TimestampFormatTrait

private const val QueryContentType: String = "application/x-www-form-urlencoded"

abstract class QueryHttpBindingProtocolGenerator : AwsHttpBindingProtocolGenerator() {
    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val queryMiddleware = listOf(
            getErrorMiddleware(ctx),
            // ensure content-type gets set
            // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#protocol-behavior
            MutateHeadersMiddleware(addMissingHeaders = mapOf("Content-Type" to QueryContentType))
        )

        return middleware + queryMiddleware
    }

    /**
     * Gets the [AbstractSerdeDescriptorGenerator] to use for deserializers.
     */
    abstract fun getDeserializerDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): AbstractSerdeDescriptorGenerator

    /**
     * Get the [ModeledExceptionsMiddleware] for this protocol.
     */
    abstract fun getErrorMiddleware(ctx: ProtocolGenerator.GenerationContext): ModeledExceptionsMiddleware

    override fun getProtocolHttpBindingResolver(model: Model, serviceShape: ServiceShape): HttpBindingResolver =
        QueryBindingResolver(model, serviceShape)

    /**
     * Gets the [AbstractSerdeDescriptorGenerator] to use for serializers.
     */
    abstract fun getSerializerDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ): AbstractSerdeDescriptorGenerator

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

        unwrapOperationResponseBody(op.id.name, writer)
        renderDeserializerBody(ctx, shape, documentMembers, writer)
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

    override fun renderSerializeDocumentBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        writer: KotlinWriter
    ) {
        renderSerializerBody(ctx, shape, shape.members().toList(), writer)
    }

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
        writer.addImport(RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
        writer.write("val serializer = #T()", RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
        renderSerializerBody(ctx, shape, documentMembers, writer)
        writer.write("return serializer.toByteArray()")
    }

    abstract fun unwrapOperationResponseBody(operationName: String, writer: KotlinWriter)
}

/**
 * An HTTP binding resolver for the query binding protocols
 */
class QueryBindingResolver(
    model: Model,
    service: ServiceShape
) : StaticHttpBindingResolver(model, service, QueryHttpTrait, QueryContentType, TimestampFormatTrait.Format.DATE_TIME) {
    constructor(ctx: ProtocolGenerator.GenerationContext) : this(ctx.model, ctx.service)

    companion object {
        val QueryHttpTrait: HttpTrait = HttpTrait
            .builder()
            .code(200)
            .method("POST")
            .uri(UriPattern.parse("/"))
            .build()
    }
}
