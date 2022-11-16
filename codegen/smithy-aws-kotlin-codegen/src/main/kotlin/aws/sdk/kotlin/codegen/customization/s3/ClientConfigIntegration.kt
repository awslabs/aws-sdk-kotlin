package aws.sdk.kotlin.codegen.customization.s3

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
 * Integration to inject s3-related client config builtins for endpoint resolution in place of the corresponding client
 * context params.
 */
class ClientConfigIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    companion object {
        val EnableAccelerateProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "enableAccelerate",
            defaultValue = false,
            documentation = """
                Flag to support [S3 transfer acceleration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/transfer-acceleration.html)
                with this client.
            """.trimIndent(),
        )

        val ForcePathStyleProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "forcePathStyle",
            defaultValue = false,
            documentation = """
                Flag to use legacy path-style addressing when making requests.
            """.trimIndent(),
        )

        val UseArnRegionProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "useArnRegion",
            defaultValue = false,
            documentation = """
                Flag to enforce using a bucket arn with a region matching the client config when making requests with
                [S3 access points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-points.html).
            """.trimIndent(),
        )

        // FUTURE: default signer doesn't yet implement sigv4a, default to mrap OFF until it does
        val DisableMrapProp: ClientConfigProperty = ClientConfigProperty.Boolean(
            "disableMrap",
            defaultValue = true,
            documentation = """
                Flag to disable [S3 multi-region access points](https://docs.aws.amazon.com/AmazonS3/latest/userguide/MultiRegionAccessPoints.html).
            """.trimIndent(),
        )
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val transformer = ModelTransformer.create()
        return transformer.mapTraits(model) { _, trait ->
            if (trait is ClientContextParamsTrait) {
                ClientContextParamsTrait.builder()
                    .parameters(trait.parameters)
                    .removeParameter("ForcePathStyle")
                    .removeParameter("UseArnRegion")
                    .removeParameter("DisableMultiRegionAccessPoints")
                    .removeParameter("Accelerate")
                    .build()
            } else {
                trait
            }
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ClientConfigProperty> =
        listOf(
            EnableAccelerateProp,
            ForcePathStyleProp,
            UseArnRegionProp,
            DisableMrapProp,
        )
}
