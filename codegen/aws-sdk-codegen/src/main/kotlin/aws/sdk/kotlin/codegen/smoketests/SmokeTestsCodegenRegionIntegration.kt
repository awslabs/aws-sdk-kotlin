package aws.sdk.kotlin.codegen.smoketests

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestDefaultConfig
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestAdditionalEnvVars
import software.amazon.smithy.kotlin.codegen.rendering.smoketests.SmokeTestRegionDefault
import software.amazon.smithy.kotlin.codegen.utils.topDownOperations
import software.amazon.smithy.model.Model
import software.amazon.smithy.smoketests.traits.SmokeTestsTrait

/**
 * Adds AWS region support to smoke tests
 */
class SmokeTestsCodegenRegionIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.topDownOperations(settings.service).any { it.hasTrait<SmokeTestsTrait>() } && settings.sdkId !in smokeTestDenyList

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(SmokeTestAdditionalEnvVars, envVars),
            SectionWriterBinding(SmokeTestDefaultConfig, region),
            SectionWriterBinding(SmokeTestRegionDefault, regionDefault),
        )

    private val envVars = SectionWriter { writer, _ ->
        writer.write("private val regionOverride = System.getenv(#S)", "AWS_SMOKE_TEST_REGION")
    }

    private val region = SectionWriter { writer, _ ->
        writer.write("region = regionOverride")
    }

    private val regionDefault = SectionWriter { writer, _ ->
        writer.write("regionOverride ?:")
    }
}