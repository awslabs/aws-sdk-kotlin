package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.codegen.core.SymbolReference
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeType

class AwsServiceConfigIntegration : KotlinIntegration {

    override val serviceClientConfigFeatures: List<ServiceConfigFeature>
        get() = listOf(AwsServiceConfigFeature())

    class AwsServiceConfigFeature : ServiceConfigFeature {
        override fun supplyConstructorParameters(model: Model, service: ServiceShape, applicationProtocol: ApplicationProtocol, writer: KotlinWriter): List<String> {
            val awsServiceConfigSymbol = Symbol.builder()
                    .name("AwsCredentialsProviders")
                    .namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE.namespace, ".")
                    .addDependency(AwsKotlinDependency.AWS_CLIENT_RT_CORE)
                    .build()
            writer.addImport(awsServiceConfigSymbol, "", SymbolReference.ContextOption.DECLARE)

            return listOf("override var credentialProviders: AwsCredentialsProviders? = null", "override var region: String? = null")
        }

        override fun supplyInterfaces(model: Model, service: ServiceShape, applicationProtocol: ApplicationProtocol, writer: KotlinWriter): List<String> {
            val awsServiceConfigSymbol = Symbol.builder()
                    .name("AwsServiceConfig")
                    .namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE.namespace, ".")
                    .addDependency(AwsKotlinDependency.AWS_CLIENT_RT_CORE)
                    .build()
            writer.addImport(awsServiceConfigSymbol, "", SymbolReference.ContextOption.DECLARE)

            return listOf("AwsServiceConfig")
        }
    }
}