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
import java.io.File

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
 * Generates an `API.md` file that will be used as module documentation in our API ref docs.
 * Some services have code example documentation we need to generate. Others have handwritten documentation.
 * The integration renders both into the `API.md` file.
 *
 * See: https://kotlinlang.org/docs/dokka-module-and-package-docs.html
 *
 * See: https://github.com/awslabs/aws-sdk-kotlin/blob/0581f5c5eeaa14dcd8af4ea0dfc088b1057f5ba5/build.gradle.kts#L68-L75
 */
class ModuleDocumentationIntegration(
    private val codeExamples: Map<String, String> = CODE_EXAMPLES_SERVICES_MAP,
) : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        codeExamples.keys.contains(
            model
                .expectShape<ServiceShape>(settings.service)
                .sdkId,
        ) ||
            handWrittenDocsFile(settings).exists()

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.fileManifest.writeFile(
            "API.md",
            generateModuleDocumentation(ctx),
        )
    }

    internal fun generateModuleDocumentation(
        ctx: CodegenContext,
    ) = buildString {
        val handWrittenDocsFile = handWrittenDocsFile(ctx.settings)
        if (handWrittenDocsFile.exists()) {
            append(
                handWrittenDocsFile.readText(),
            )
            appendLine()
        }
        if (codeExamples.keys.contains(ctx.settings.sdkId)) {
            if (!handWrittenDocsFile.exists()) {
                append(
                    boilerPlate(ctx),
                )
            }
            append(
                codeExamplesDocs(ctx),
            )
        }
    }

    private fun boilerPlate(ctx: CodegenContext) = buildString {
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

    private fun codeExamplesDocs(ctx: CodegenContext) = buildString {
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
}

private fun handWrittenDocsFile(settings: KotlinSettings): File {
    val sdkRootDir = System.getProperty("user.dir")
    val serviceDir = "$sdkRootDir/services/${settings.pkg.name.split(".").last()}"

    return File("$serviceDir/DOCS.md")
}
