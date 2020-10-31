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

            writer.onSection(SECTION_SERVICE_CONFIG_PROPERTIES) {
                writer.write(it)
                writer.write("override val credentialProviders: AwsCredentialsProviders? = builder.credentialProviders")
                writer.write("override val region: String? = builder.region")
                writer.write("override val signingRegion: String? = builder.signingRegion")
            }

            writer.onSection(SECTION_SERVICE_CONFIG_BUILDER_BODY) {
                writer.write(it)
                writer.write("fun credentialProviders(credentialProviders: AwsCredentialsProviders): Builder")
                writer.write("fun region(region: String): Builder")
                writer.write("fun signingRegion(signingRegion: String): Builder")
            }

            writer.onSection(SECTION_SERVICE_CONFIG_DSL_BUILDER_BODY) {
                writer.write(it)
                writer.write("var credentialProviders: AwsCredentialsProviders?")
                writer.write("var region: String?")
                writer.write("var signingRegion: String?")
            }

            writer.onSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_PROPERTIES) {
                writer.write(it)
                writer.write("override var credentialProviders: AwsCredentialsProviders? = null")
                writer.write("override var region: String? = null")
                writer.write("override var signingRegion: String? = null")
            }

            writer.onSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_CONSTRUCTOR) {
                writer.write(it)
                writer.write("this.credentialProviders = config.credentialProviders")
                writer.write("this.region = config.region")
                writer.write("this.signingRegion = config.signingRegion")
            }

            writer.onSection(SECTION_SERVICE_CONFIG_BUILDER_IMPL_BODY) {
                writer.write(it)
                writer.write("override fun credentialProviders(credentialProviders: AwsCredentialsProviders): Builder = apply { this.credentialProviders = credentialProviders }")
                writer.write("override fun region(region: String): Builder = apply { this.region = region }")
                writer.write("override fun signingRegion(signingRegion: String): Builder = apply { this.signingRegion = signingRegion }")
            }
        }

        super.onShapeWriterUse(settings, model, symbolProvider, writer, definedShape)
    }
}