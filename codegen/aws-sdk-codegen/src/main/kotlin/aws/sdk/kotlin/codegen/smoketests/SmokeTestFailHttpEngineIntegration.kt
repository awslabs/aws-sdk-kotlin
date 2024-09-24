package aws.sdk.kotlin.codegen.smoketests

import aws.sdk.kotlin.codegen.model.traits.FailedResponseTrait
import aws.sdk.kotlin.codegen.model.traits.SuccessResponseTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestHttpEngineOverride
import software.amazon.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

/**
 * Adds [FailedResponseTrait] support to smoke tests
 */
class SmokeTestFailHttpEngineIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.topDownOperations(settings.service).any { it.hasTrait<SmokeTestsTrait>() } &&
            settings.sdkId !in smokeTestDenyList &&
            !model.expectShape<ServiceShape>(settings.service).hasTrait(SuccessResponseTrait.ID) &&
            model.expectShape<ServiceShape>(settings.service).hasTrait(FailedResponseTrait.ID)

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(SmokeTestHttpEngineOverride, httpClientOverride),
        )

    private val httpClientOverride = SectionWriter { writer, _ ->
        writer.withBlock("httpClient = #T(", ")", RuntimeTypes.HttpTest.TestEngine) {
            withBlock("roundTripImpl = { _, request ->", "}") {
                write(
                    "val resp = #T(#T.BadRequest, #T.Empty, #T.Empty)",
                    RuntimeTypes.Http.Response.HttpResponse,
                    RuntimeTypes.Http.StatusCode,
                    RuntimeTypes.Http.Headers,
                    RuntimeTypes.Http.HttpBody,
                )
                write("val now = #T.now()", RuntimeTypes.Core.Instant)
                write("#T(request, resp, now, now)", RuntimeTypes.Http.HttpCall)
            }
        }
    }
}
