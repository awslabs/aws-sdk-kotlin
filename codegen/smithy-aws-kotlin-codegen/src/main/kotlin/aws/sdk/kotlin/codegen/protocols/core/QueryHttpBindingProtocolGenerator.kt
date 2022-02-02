package aws.sdk.kotlin.codegen.protocols.core

import aws.sdk.kotlin.codegen.protocols.middleware.MutateHeadersMiddleware
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.knowledge.SerdeIndex
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
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
            // ensure content-type gets set
            // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#protocol-behavior
            MutateHeadersMiddleware(addMissingHeaders = mapOf("Content-Type" to QueryContentType))
        )

        return middleware + queryMiddleware
    }

    override fun getProtocolHttpBindingResolver(model: Model, serviceShape: ServiceShape): HttpBindingResolver =
        QueryBindingResolver(model, serviceShape)

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
            val action = op.id.name
            val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
            val version = service.version
            writer.write("""val content = "Action=$action&Version=$version"""")
            writer.write("builder.body = #T(content.encodeToByteArray())", RuntimeTypes.Http.ByteArrayContent)
        } else {
            super.renderSerializeHttpBody(ctx, op, writer)
        }
    }
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

abstract class AbstractQueryFormUrlSerializerGenerator(
    private val protocolGenerator: ProtocolGenerator,
    private val defaultTimestampFormat: TimestampFormatTrait.Format
) : StructuredDataSerializerGenerator {

    abstract fun descriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter
    ): FormUrlSerdeDescriptorGenerator

    override fun operationSerializer(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, members: List<MemberShape>): Symbol {
        val input = op.input.get().let { ctx.model.expectShape(it) }
        val symbol = ctx.symbolProvider.toSymbol(input)

        return op.bodySerializer(ctx.settings) { writer ->
            addNestedDocumentSerializers(ctx, op, writer)
            val fnName = op.bodySerializerName()
            writer.openBlock("private fun #L(context: #T, input: #T): ByteArray {", fnName, RuntimeTypes.Core.ExecutionContext, symbol)
                .call {
                    renderSerializeOperationBody(ctx, op, members, writer)
                }
                .closeBlock("}")
        }
    }

    /**
     * Register nested structure/map shapes reachable from the operation input shape that require a "document" serializer
     * implementation
     */
    private fun addNestedDocumentSerializers(ctx: ProtocolGenerator.GenerationContext, shape: Shape, writer: KotlinWriter) {
        val serdeIndex = SerdeIndex.of(ctx.model)
        val shapesRequiringDocumentSerializer = serdeIndex.requiresDocumentSerializer(shape)
        // register a dependency on each of the members that require a serializer impl
        // ensuring they get generated
        shapesRequiringDocumentSerializer.forEach {
            val nestedStructOrUnionSerializer = documentSerializer(ctx, it)
            writer.addImport(nestedStructOrUnionSerializer)
        }
    }

    private fun renderSerializeOperationBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        documentMembers: List<MemberShape>,
        writer: KotlinWriter
    ) {
        val shape = ctx.model.expectShape(op.input.get())
        writer.write("val serializer = #T()", RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
        renderSerializerBody(ctx, shape, documentMembers, writer)
        writer.write("return serializer.toByteArray()")
    }

    private fun documentSerializer(ctx: ProtocolGenerator.GenerationContext, shape: Shape): Symbol {
        val symbol = ctx.symbolProvider.toSymbol(shape)
        return symbol.documentSerializer(ctx.settings) { writer ->
            val fnName = symbol.documentSerializerName()
            writer.openBlock("internal fun #L(serializer: #T, input: #T) {", fnName, RuntimeTypes.Serde.Serializer, symbol)
                .call {
                    renderSerializerBody(ctx, shape, shape.members().toList(), writer)
                }
                .closeBlock("}")
        }
    }

    private fun renderSerializerBody(
        ctx: ProtocolGenerator.GenerationContext,
        shape: Shape,
        members: List<MemberShape>,
        writer: KotlinWriter,
    ) {
        // render the serde descriptors
        descriptorGenerator(ctx, shape, members, writer).render()
        if (shape.isUnionShape) {
            SerializeUnionGenerator(ctx, members, writer, defaultTimestampFormat).render()
        } else {
            SerializeStructGenerator(ctx, members, writer, defaultTimestampFormat).render()
        }
    }

    override fun payloadSerializer(ctx: ProtocolGenerator.GenerationContext, member: MemberShape): Symbol {
        // re-use document serializer (for the target shape!)
        val target = ctx.model.expectShape(member.target)
        val symbol = ctx.symbolProvider.toSymbol(member)
        val serializeFn = documentSerializer(ctx, target)
        val fnName = symbol.payloadSerializerName()
        return symbol.payloadSerializer(ctx.settings) { writer ->
            addNestedDocumentSerializers(ctx, target, writer)
            writer.withBlock("internal fun #L(input: #T): ByteArray {", "}", fnName, symbol) {
                write("val serializer = #T()", RuntimeTypes.Serde.SerdeFormUrl.FormUrlSerializer)
                write("#T(serializer, input)", serializeFn)
                write("return serializer.toByteArray()")
            }
        }
    }
}
