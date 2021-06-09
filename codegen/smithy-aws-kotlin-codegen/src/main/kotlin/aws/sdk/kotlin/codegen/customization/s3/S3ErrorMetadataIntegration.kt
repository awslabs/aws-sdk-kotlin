package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlErrorMiddleware
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.ExceptionBaseClassGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.utils.namespaceToPath
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Generates an S3-specific subclass of AwsErrorMetadata.
 */
class S3ErrorMetadataIntegration : KotlinIntegration {

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(ExceptionBaseClassGenerator.ExceptionBaseClassSection, addSdkErrorMetadataWriter)
        )

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val writer = KotlinWriter("${ctx.settings.pkg.name}.model")

        writer.addImport(AwsRuntimeTypes.Core.AwsErrorMetadata)
        writer.addImport(RuntimeTypes.Utils.AttributeKey)

        writer.withBlock("public class S3ErrorMetadata : AwsErrorMetadata() {", "}") {
            writer.withBlock("public companion object {", "}") {
                writer.write("""public val RequestId2: AttributeKey<String> = AttributeKey("S3:RequestId2")""")
            }

            writer
                .write("public val requestId2: String?")
                .indent()
                .write("get() = attributes.getOrNull(RequestId2)")
                .dedent()
        }

        val contents = writer.toString()

        val packagePath = ctx.settings.pkg.name.namespaceToPath()
        delegator.fileManifest.writeFile("$DEFAULT_SOURCE_SET_ROOT$packagePath/model/S3ErrorMetadata.kt", contents)

        val kotlinTestDependency = KotlinDependency(
            GradleConfiguration.TestImplementation,
            "org.jetbrains.kotlin",
            "org.jetbrains.kotlin",
            "kotlin-test",
            "\$kotlinVersion"
        )

        delegator.runtimeDependencies.addAll(kotlinTestDependency.dependencies)
        delegator.runtimeDependencies.addAll(AwsKotlinDependency.AWS_CLIENT_RT_TESTING.dependencies)
    }

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> =
        resolved.replace(newValue = S3ErrorMiddleware(ctx, HttpTraitResolver(ctx, "application/xml"))) {
            it is RestXmlErrorMiddleware
        }

    // SectionWriter to override the default sdkErrorMetadata for S3's version
    private val addSdkErrorMetadataWriter = SectionWriter { writer, _ ->
        writer.write("override val sdkErrorMetadata: S3ErrorMetadata = S3ErrorMetadata()")
    }
}
