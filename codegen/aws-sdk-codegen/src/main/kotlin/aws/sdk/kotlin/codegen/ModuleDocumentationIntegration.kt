package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.TitleTrait
import java.io.IOException

/**
 * Maps a service's SDK ID to its code examples
 */
private val CODE_EXAMPLES_SERVICES_MAP = mapOf(
    "API Gateway" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_api-gateway_code_examples.html",
    "Auto Scaling" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_auto-scaling_code_examples.html",
    "Bedrock" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_bedrock_code_examples.html",
    "CloudWatch" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_cloudwatch_code_examples.html",
    "Comprehend" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_comprehend_code_examples.html",
    "DynamoDB" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_dynamodb_code_examples.html",
    "EC2" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_ec2_code_examples.html",
    "ECR" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_ecr_code_examples.html",
    "OpenSearch" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_opensearch_code_examples.html",
    "EventBridge" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_eventbridge_code_examples.html",
    "Glue" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_glue_code_examples.html",
    "IAM" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_iam_code_examples.html",
    "IoT" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_iot_code_examples.html ",
    "Keyspaces" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_keyspaces_code_examples.html",
    "KMS" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_kms_code_examples.html",
    "Lambda" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_lambda_code_examples.html",
    "MediaConvert" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_mediaconvert_code_examples.html",
    "Pinpoint" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_pinpoint_code_examples.html",
    "RDS" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_rds_code_examples.html",
    "Redshift" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_redshift_code_examples.html",
    "Rekognition" to "https://docs.aws.amazon.com/code-library/latest/ug/kotlin_1_rekognition_code_examples.html",
)

/**
 * Maps a service's SDK ID to its handwritten module documentation file in the `resources` dir.
 * The module documentation files MUST be markdown files.
 */
private val HAND_WRITTEN_SERVICES_MAP = mapOf(
    "S3" to "S3.md",
)

/**
 * Generates an `API.md` file that will be used as module documentation in our API ref docs.
 * Some services have code examples we need to link to. Others have handwritten documentation.
 * The integration renders both into the `API.md` file.
 *
 * See: https://kotlinlang.org/docs/dokka-module-and-package-docs.html
 *
 * See: https://github.com/awslabs/aws-sdk-kotlin/blob/0581f5c5eeaa14dcd8af4ea0dfc088b1057f5ba5/build.gradle.kts#L68-L75
 */
class ModuleDocumentationIntegration(
    private val codeExamples: Map<String, String> = CODE_EXAMPLES_SERVICES_MAP,
    private val handWritten: Map<String, String> = HAND_WRITTEN_SERVICES_MAP,
) : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).sdkId.let {
            codeExamples.keys.contains(it) || handWritten.keys.contains(it)
        }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.fileManifest.writeFile(
            "API.md",
            generateModuleDocumentation(ctx, ctx.settings.sdkId),
        )
    }

    internal fun generateModuleDocumentation(
        ctx: CodegenContext,
        sdkId: String,
    ) = buildString {
        append(
            generateBoilerPlate(ctx),
        )
        if (handWritten.keys.contains(sdkId)) {
            append(
                generateHandWrittenDocs(sdkId),
            )
            appendLine()
        }
        if (codeExamples.keys.contains(sdkId)) {
            append(
                generateCodeExamplesDocs(ctx),
            )
        }
    }

    private fun generateBoilerPlate(ctx: CodegenContext) = buildString {
        // Title must be "Module" followed by the exact module name or dokka won't render it
        appendLine("# Module ${ctx.settings.pkg.name.split(".").last()}")
        appendLine()
        ctx
            .model
            .expectShape<ServiceShape>(ctx.settings.service)
            .getTrait<TitleTrait>()
            ?.value
            ?.let {
                appendLine(it)
                appendLine()
            }
    }

    private fun generateCodeExamplesDocs(ctx: CodegenContext) = buildString {
        val sdkId = ctx.settings.sdkId
        val codeExampleLink = codeExamples[sdkId]
        val title = ctx
            .model
            .expectShape<ServiceShape>(ctx.settings.service)
            .getTrait<TitleTrait>()
            ?.value

        appendLine("## Code Examples")
        append("To see full code examples, see the ${title ?: sdkId} examples in the AWS code example library. ")
        appendLine("See $codeExampleLink")
        appendLine()
    }

    private fun generateHandWrittenDocs(sdkId: String): String = object {}
        .javaClass
        .classLoader
        .getResourceAsStream("aws/sdk/kotlin/codegen/moduledocumentation/${handWritten[sdkId]}")
        ?.bufferedReader()
        ?.readText()
        ?: throw IOException("Unable to read from file ${handWritten[sdkId]}")
}
