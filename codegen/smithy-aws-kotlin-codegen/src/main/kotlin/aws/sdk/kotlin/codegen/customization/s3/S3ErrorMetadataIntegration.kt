package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration

class S3ErrorMetadataIntegration : KotlinIntegration {

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

        writer.write("\n// Add property to S3Exception type")
        writer.write("val S3Exception.sdkErrorMetadata: S3ErrorMetadata")
        writer.indent()
        writer.write("get() = S3ErrorMetadata()")
        writer.dedent()

        val contents = writer.toString()

        val packagePath = ctx.settings.pkg.name.replace('.', '/')
        delegator.fileManifest.writeFile("src/main/kotlin/$packagePath/model/S3ErrorMetadata.kt", contents)
    }
}