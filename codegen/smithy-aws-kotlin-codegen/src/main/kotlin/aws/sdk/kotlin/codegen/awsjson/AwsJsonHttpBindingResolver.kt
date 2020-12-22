package aws.sdk.kotlin.codegen.awsjson

import software.amazon.smithy.kotlin.codegen.integration.HttpBindingDescriptor
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.shapes.ToShapeId
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.traits.TimestampFormatTrait

class AwsJsonHttpBindingResolver(
    private val generationContext: ProtocolGenerator.GenerationContext,
    private val topDownIndex: TopDownIndex = TopDownIndex.of(generationContext.model)
): HttpBindingResolver {
    // A non-model source instance of HttpTrait w/ static properties used in awsJson protocols.
    private val AWS_REST_HTTP_TRAIT: HttpTrait = HttpTrait
        .builder()
        .code(200).
        method("POST")
        .uri(UriPattern.parse("/"))
        .build()

    override fun resolveBindingOperations(): List<OperationShape> =
        topDownIndex.getContainedOperations(generationContext.service).toList()

    override fun resolveHttpTrait(operationShape: OperationShape): HttpTrait = AWS_REST_HTTP_TRAIT

    override fun resolveRequestBindings(operationShape: OperationShape): List<HttpBindingDescriptor> {
        val inputs = generationContext.model.expectShape(operationShape.input.get())

        // TODO consider factory function from MemberShape
        return inputs.members().map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT, "")  }.toList()
    }

    override fun resolveResponseBindings(shapeId: ShapeId): List<HttpBindingDescriptor> {
        return when (val shape = generationContext.model.expectShape(shapeId)) {
            is OperationShape -> {
                val outputs = generationContext.model.expectShape(shape.output.get())

                outputs.members().map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT, "")  }.toList()
            }
            is StructureShape -> {
                val outputs = shape.members()

                outputs.map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT, "")  }.toList()
            }
            else -> {
                error("Unimplemented shape type for http response bindings: $shape")
            }
        }
    }

    override fun determineRequestContentType(operationShape: OperationShape): String = "application/x-amz-json-1.0"

    // TODO consider passing function to return timestamp format
    override fun determineTimestampFormat(
        member: ToShapeId?,
        location: HttpBinding.Location?,
        defaultFormat: TimestampFormatTrait.Format?
    ): TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS
}