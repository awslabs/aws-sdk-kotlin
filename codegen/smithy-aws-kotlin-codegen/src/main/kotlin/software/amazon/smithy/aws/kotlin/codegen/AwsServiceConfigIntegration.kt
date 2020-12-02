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
            writer.addImport("CredentialsProvider", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
            writer.addImport("RegionConfig", AwsKotlinDependency.AWS_CLIENT_RT_REGIONS)

            writer.onSection(SECTION_SERVICE_CONFIG_PARENT_TYPE) { text -> writer.appendWithDelimiter(text, "AuthConfig") }
            writer.onSection(SECTION_SERVICE_CONFIG_PARENT_TYPE) { text -> writer.appendWithDelimiter(text, "RegionConfig") }

            writer.appendToSection(SECTION_SERVICE_CONFIG_PROPERTIES) {
                write("override val credentialsProvider: CredentialsProvider? = builder.credentialsProvider")
                write("override val region: String? = builder.region")
                write("override val signingRegion: String? = builder.signingRegion")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_BODY) {
                write("fun credentialsProvider(credentialsProvider: CredentialsProvider): Builder")
                write("fun region(region: String): Builder")
                write("fun signingRegion(signingRegion: String): Builder")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_DSL_BUILDER_BODY) {
                write("var credentialsProvider: CredentialsProvider?")
                write("var region: String?")
                write("var signingRegion: String?")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_PROPERTIES) {
                write("override var credentialsProvider: CredentialsProvider? = null")
                write("override var region: String? = null")
                write("override var signingRegion: String? = null")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_CONSTRUCTOR) {
                write("this.credentialsProvider = config.credentialsProvider")
                write("this.region = config.region")
                write("this.signingRegion = config.signingRegion")
            }

            writer.appendToSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_BODY) {
                write("override fun credentialsProvider(credentialsProvider: CredentialsProvider): Builder = apply { this.credentialsProvider = credentialsProvider }")
                write("override fun region(region: String): Builder = apply { this.region = region }")
                write("override fun signingRegion(signingRegion: String): Builder = apply { this.signingRegion = signingRegion }")
            }
        }

        super.onShapeWriterUse(settings, model, symbolProvider, writer, definedShape)
    }
}
