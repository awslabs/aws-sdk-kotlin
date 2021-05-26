package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.protocols.RestXml
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlErrorMiddleware
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpTraitResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.ServiceShape

class S3ErrorMetadataIntegration : KotlinIntegration {

    override fun apply(service: ServiceShape) = service.isS3

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val writer = KotlinWriter("${ctx.settings.pkg.name}.model")

        writer.addImport("AwsErrorMetadata", AwsKotlinDependency.AWS_CLIENT_RT_CORE)
        writer.addImport("AttributeKey", KotlinDependency.CLIENT_RT_UTILS)

        writer.withBlock("public open class S3ErrorMetadata : AwsErrorMetadata() {", "}") {
            writer.withBlock("public companion object {", "}") {
                writer.write("""public val RequestId2: AttributeKey<String> = AttributeKey("S3:RequestId2")""")
            }

            writer.write("public val requestId2: String?")
            writer.indent()
            writer.write("get() = attributes.getOrNull(RequestId2)")
            writer.dedent()
        }

        val contents = writer.toString()

        val packagePath = ctx.settings.pkg.name.replace('.', '/')
        delegator.fileManifest.writeFile("src/main/kotlin/$packagePath/model/S3ErrorMetadata.kt", contents)
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        generator: HttpBindingProtocolGenerator,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> =
        resolved.replace(newValue = S3ErrorMiddleware(ctx, generator.getProtocolHttpBindingResolver(ctx))) {
            it is RestXmlErrorMiddleware
        }

    override fun augmentBaseErrorType(writer: KotlinWriter) {
        writer.write("override val sdkErrorMetadata: S3ErrorMetadata = S3ErrorMetadata()")
    }
}