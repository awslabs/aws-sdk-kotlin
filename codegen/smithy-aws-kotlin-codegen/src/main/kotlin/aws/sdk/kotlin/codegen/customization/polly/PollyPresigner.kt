package aws.sdk.kotlin.codegen.customization.polly

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.PresignerGenerator
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinDependency
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Override the PresignedRequestConfig instance generation for Polly based on customization SEP
 */
class PollyPresigner : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).id.toString() == "com.amazonaws.polly#Parrot_v1"

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(PresignerGenerator.PresignConfigFnSection, addPollyPresignConfigFnWriter))

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.runtimeDependencies.addAll(KotlinDependency.KOTLIN_TEST.dependencies)
        delegator.runtimeDependencies.addAll(AwsKotlinDependency.AWS_TESTING.dependencies)
    }

    private val addPollyPresignConfigFnWriter = SectionWriter { writer, _ ->
        val model = writer.getContext(PresignerGenerator.PresignConfigFnSection.Model) as Model
        val symbolProvider = writer.getContext(PresignerGenerator.PresignConfigFnSection.SymbolProvider) as SymbolProvider
        val operation = model.expectShape<OperationShape>(writer.getContext(PresignerGenerator.PresignConfigFnSection.OperationId) as String)

        val opInput = model.expectShape(operation.input.get())
        writer.addImport(RuntimeTypes.Http.QueryParametersBuilder)
        writer.addImport(RuntimeTypes.Http.HttpMethod)
        writer.write(
            """            
            require(durationSeconds > 0u) { "duration must be greater than zero" }
            val httpRequestBuilder = SynthesizeSpeechOperationSerializer().serialize(ExecutionContext.build { }, request)
            val queryStringBuilder = QueryParametersBuilder()
            """.trimIndent()
        )

        opInput.members().forEach { memberShape ->
            val name = symbolProvider.toMemberName(memberShape)
            val type = memberShape.id.member.get()
            writer.openBlock("if (request.$name != null) {", "}") {
                writer.write("queryStringBuilder.append(\"$type\", request.$name.toString())")
            }
        }

        writer.write(
            """
            return PresignedRequestConfig(
                HttpMethod.GET,
                httpRequestBuilder.url.path,
                queryStringBuilder.build(),
                durationSeconds.toLong(),
                false,
                SigningLocation.QUERY_STRING
            )
            """.trimIndent()
        )
    }
}
