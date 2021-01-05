package aws.sdk.kotlin.codegen.awsjson

import software.amazon.smithy.kotlin.codegen.integration.HttpBindingDescriptor
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.HttpTrait
import software.amazon.smithy.model.traits.TimestampFormatTrait

/**
 * An HTTP binding resolver for the awsJson protocol.
 */
class AwsJsonHttpBindingResolver(
    private val generationContext: ProtocolGenerator.GenerationContext,
    private val defaultContentType: String,
    private val topDownIndex: TopDownIndex = TopDownIndex.of(generationContext.model)
) : HttpBindingResolver {

    companion object {
        // A non-model source instance of HttpTrait w/ static properties used in awsJson protocols.
        // TODO ~ link to future awsJson spec which describes these static attributes
        private val awsJsonHttpTrait: HttpTrait = HttpTrait
            .builder()
            .code(200)
            .method("POST")
            .uri(UriPattern.parse("/"))
            .build()
    }

    /**
     * All operations are binding for awsJson model.
     */
    override fun bindingOperations(): List<OperationShape> =
        topDownIndex.getContainedOperations(generationContext.service).toList()

    /**
     * The HttpTrait is not attached or otherwise associated with awsJson models.  But
     * because for awsJson these data elements are static, we can supply an instance
     * that provides the necessary details to drive codegen.
     */
    override fun httpTrait(operationShape: OperationShape): HttpTrait = awsJsonHttpTrait

    /**
     * Returns all inputs as Document bindings for awsJson protocol.
     */
    override fun requestBindings(operationShape: OperationShape): List<HttpBindingDescriptor> {
        if (!operationShape.input.isPresent) return emptyList()

        val inputs = generationContext.model.expectShape(operationShape.input.get())

        return inputs.members().map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT, "") }.toList()
    }

    /**
     * Returns all outputs as Document bindings for awsJson protocol.
     */
    override fun responseBindings(shape: Shape): List<HttpBindingDescriptor> {
        return when (shape) {
            is OperationShape -> {
                if (!shape.output.isPresent) return emptyList()

                val outputs = generationContext.model.expectShape(shape.output.get())

                outputs.members().map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT, "") }.toList()
            }
            is StructureShape -> {
                val outputs = shape.members()

                outputs.map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT, "") }.toList()
            }
            else -> {
                error("Unimplemented shape type for http response bindings: $shape")
            }
        }
    }

    // TODO ~ link to future awsJson spec which describes this content type
    override fun determineRequestContentType(operationShape: OperationShape): String = defaultContentType

    override fun determineTimestampFormat(
        member: ToShapeId,
        location: HttpBinding.Location,
        defaultFormat: TimestampFormatTrait.Format
    ): TimestampFormatTrait.Format = TimestampFormatTrait.Format.EPOCH_SECONDS
}
