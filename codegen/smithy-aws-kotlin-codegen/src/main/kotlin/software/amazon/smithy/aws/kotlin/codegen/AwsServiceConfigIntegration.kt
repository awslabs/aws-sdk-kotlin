package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.ShapeType

class AwsServiceConfigIntegration : KotlinIntegration {

    override fun onShapeWriterUse(
        settings: KotlinSettings,
        model: Model,
        symbolProvider: SymbolProvider,
        writer: KotlinWriter,
        definedShape: Shape
    ) {
        if (definedShape.type == ShapeType.SERVICE) {
            writer.addImport("AuthConfig", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
            writer.addImport("AwsCredentialsProviders", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
            writer.addImport("RegionConfig", AwsKotlinDependency.AWS_CLIENT_RT_REGIONS)

            writer.onSection(SECTION_SERVICE_CONFIG_PARENT_TYPE) { text -> writer.appendWithDelimiter(text, "AuthConfig") }
            writer.onSection(SECTION_SERVICE_CONFIG_PARENT_TYPE) { text -> writer.appendWithDelimiter(text, "RegionConfig") }

            writer.appendToSection(SECTION_SERVICE_CONFIG_PROPERTIES) {
                write("override val credentialProviders: AwsCredentialsProviders? = builder.credentialProviders")
                write("override val region: String? = builder.region")
                write("override val signingRegion: String? = builder.signingRegion")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_BODY) {
                write("fun credentialProviders(credentialProviders: AwsCredentialsProviders): Builder")
                write("fun region(region: String): Builder")
                write("fun signingRegion(signingRegion: String): Builder")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_DSL_BUILDER_BODY) {
                write("var credentialProviders: AwsCredentialsProviders?")
                write("var region: String?")
                write("var signingRegion: String?")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_PROPERTIES) {
                write("override var credentialProviders: AwsCredentialsProviders? = null")
                write("override var region: String? = null")
                write("override var signingRegion: String? = null")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_CONSTRUCTOR) {
                write("this.credentialProviders = config.credentialProviders")
                write("this.region = config.region")
                write("this.signingRegion = config.signingRegion")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_BODY) {
                write("override fun credentialProviders(credentialProviders: AwsCredentialsProviders): Builder = apply { this.credentialProviders = credentialProviders }")
                write("override fun region(region: String): Builder = apply { this.region = region }")
                write("override fun signingRegion(signingRegion: String): Builder = apply { this.signingRegion = signingRegion }")
            }
        }

        super.onShapeWriterUse(settings, model, symbolProvider, writer, definedShape)
    }
}
