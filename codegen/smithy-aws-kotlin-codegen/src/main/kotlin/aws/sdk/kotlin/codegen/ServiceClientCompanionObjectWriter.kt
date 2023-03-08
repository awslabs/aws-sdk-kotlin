package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator

/**
 * Overrides the service client companion object for how a client is constructed, with the ability to extend inside the
 * declaration itself.
 */
class ServiceClientCompanionObjectWriter(private val extend: (KotlinWriter.() -> Unit)? = null) : SectionWriter {
    override fun write(writer: KotlinWriter, previousValue: String?) {
        val serviceSymbol = writer.getContextValue(ServiceClientGenerator.Sections.CompanionObject.ServiceSymbol)

        writer.withBlock(
            "public companion object : #T<Config, Config.Builder, #T, Builder>() {",
            "}",
            AwsRuntimeTypes.Config.AbstractAwsSdkClientFactory,
            serviceSymbol,
        ) {
            write("@#T", KotlinTypes.Jvm.JvmStatic)
            write("override fun builder(): Builder = Builder()")

            extend?.let {
                write("")
                it()
            }
        }
    }
}
