package aws.sdk.kotlin.codegen.customization.s3control

import aws.sdk.kotlin.codegen.customization.s3.isS3Control
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.rulesengine.traits.ClientContextParamsTrait

/**
 * Integration to inject s3control-related client config builtins for endpoint resolution in place of the corresponding
 * client context params.
 */
class ClientConfigIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3Control

    companion object {
        val UseArnRegionProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "useArnRegion",
            defaultValue = false,
            documentation = """
            Flag to enforce using a bucket arn with a region matching the client config when making requests with
            [S3 access points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html).
            """.trimIndent(),
        )
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()
        return transformer.mapTraits(model) { _, trait ->
            if (trait is ClientContextParamsTrait) {
                ClientContextParamsTrait.builder()
                    .parameters(trait.parameters)
                    .removeParameter("UseArnRegion")
                    .build()
            } else {
                trait
            }
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> =
        listOf(
            UseArnRegionProp,
        )
}
