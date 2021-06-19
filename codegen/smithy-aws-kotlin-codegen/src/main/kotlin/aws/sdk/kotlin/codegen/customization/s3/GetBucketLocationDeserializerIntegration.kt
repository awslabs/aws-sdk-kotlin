package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Registers an integration that replaces the codegened GetBucketLocationDeserializer with
 * custom deserialization logic.
 */
class GetBucketLocationDeserializerIntegration : KotlinIntegration {
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(HttpProtocolClientGenerator.OperationDeserializer, overrideGetBucketLocationDeserializerWriter))

    private val overrideGetBucketLocationDeserializerWriter = SectionWriter { writer, default ->
        val op: OperationShape = checkNotNull(writer.getContext(HttpProtocolClientGenerator.OperationDeserializer.Operation) as OperationShape?) {
            "Expected ${HttpProtocolClientGenerator.OperationDeserializer.Operation} key in context."
        }

        if (op.id.name == "GetBucketLocation") {
            writer.write("deserializer = aws.sdk.kotlin.service.s3.internal.GetBucketLocationOperationDeserializer()")
        } else {
            writer.write(default)
        }
    }
}
