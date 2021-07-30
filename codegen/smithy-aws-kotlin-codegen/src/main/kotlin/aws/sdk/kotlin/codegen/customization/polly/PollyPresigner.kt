package aws.sdk.kotlin.codegen.customization.polly

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.PresignerGenerator
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
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Add unit test dependencies for Polly's handwritten customizations
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
        writer.addImport(RuntimeTypes.Http.QueryParametersBuilder)
        writer.addImport(RuntimeTypes.Http.HttpMethod)
        writer.write(
            """
                require(durationSeconds > 0u) { "duration must be greater than zero" }
                val httpRequestBuilder = SynthesizeSpeechOperationSerializer().serialize(ExecutionContext.build { }, request)
                val queryStringBuilder = QueryParametersBuilder()
                if (request.engine != null) {
                    queryStringBuilder.append("Engine", request.engine.toString())
                }
                if (request.languageCode != null) {
                    queryStringBuilder.append("LanguageCode", request.languageCode.toString())
                }
                if (request.lexiconNames != null) {
                    queryStringBuilder.append("LexiconNames", request.lexiconNames.toString())
                }
                if (request.outputFormat != null) {
                    queryStringBuilder.append("OutputFormat", request.outputFormat.toString())
                }
                if (request.sampleRate != null) {
                    queryStringBuilder.append("SampleRate", request.sampleRate.toString())
                }
                if (request.speechMarkTypes != null) {
                    queryStringBuilder.append("SpeechMarkTypes", request.speechMarkTypes.toString())
                }
                if (request.text != null) {
                    queryStringBuilder.append("Text", request.text.toString())
                }
                if (request.textType != null) {
                    queryStringBuilder.append("TextType", request.textType.toString())
                }
                if (request.voiceId != null) {
                    queryStringBuilder.append("VoiceId", request.voiceId.toString())
                }
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
