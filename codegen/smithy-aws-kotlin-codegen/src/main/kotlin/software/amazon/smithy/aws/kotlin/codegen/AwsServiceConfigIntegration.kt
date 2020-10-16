package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.codegen.core.SymbolReference
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeType

class AwsServiceConfigIntegration : KotlinIntegration {

    override val serviceClientConfigFeatures: List<ServiceConfigFeature>
        get() = listOf(
                ServiceConfigFeature { _, _, _, writer ->
                    val awsServiceConfigSymbol = Symbol.builder()
                            .name("AwsServiceConfig")
                            .namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE.namespace, ".")
                            .addDependency(AwsKotlinDependency.AWS_CLIENT_RT_CORE)
                            .build()
                    writer.addImport(awsServiceConfigSymbol, "", SymbolReference.ContextOption.DECLARE)
                    writer.write("var serviceConfig: AwsServiceConfig? = null")
                }
        )
}