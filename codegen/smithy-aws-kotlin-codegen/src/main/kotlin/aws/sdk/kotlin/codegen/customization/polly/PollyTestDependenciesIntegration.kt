package aws.sdk.kotlin.codegen.customization.polly

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinDependency
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Add unit test dependencies for Polly's handwritten customizations
 */
class PollyTestDependenciesIntegration : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).id.toString() == "com.amazonaws.polly#Parrot_v1"

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.runtimeDependencies.addAll(KotlinDependency.KOTLIN_TEST.dependencies)
        delegator.runtimeDependencies.addAll(AwsKotlinDependency.AWS_TESTING.dependencies)
    }
}
