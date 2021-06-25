package aws.sdk.kotlin.codegen.customization.polly

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.Kotlin3PDependency
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlErrorMiddleware
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.utils.namespaceToPath
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Adds necessary dependencies for Polly presigner runtime code.
 */
class PollyPresignerIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).id.toString() == "com.amazonaws.polly#Parrot_v1"

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.runtimeDependencies.addAll(Kotlin3PDependency.KOTLIN_TEST.dependencies)
        delegator.runtimeDependencies.addAll(AwsKotlinDependency.AWS_TESTING.dependencies)
    }
}
