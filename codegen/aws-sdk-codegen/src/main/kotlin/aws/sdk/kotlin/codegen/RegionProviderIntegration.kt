package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.asNullable
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty

/**
 * Adds region provider integration to the client config
 */
class RegionProviderIntegration : KotlinIntegration {
    companion object {
        val RegionProviderProp: ConfigProperty = ConfigProperty {
            name = "regionProvider"
            symbol = AwsRuntimeTypes.Config.Region.RegionProvider.asNullable()
            baseClass = AwsRuntimeTypes.Config.AwsSdkClientConfig
            useNestedBuilderBaseClass()
            documentation = """
              An optional region provider that determines the AWS region for client operations. When specified, this provider 
              takes precedence over the default region provider chain, unless a static region is explicitly configured. 

              The region resolution order is:
              1. Static region (if specified)
              2. Custom region provider (if configured)
              3. Default region provider chain
            """.trimIndent()
        }
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = buildList {
        add(RegionProviderProp)
    }
}
