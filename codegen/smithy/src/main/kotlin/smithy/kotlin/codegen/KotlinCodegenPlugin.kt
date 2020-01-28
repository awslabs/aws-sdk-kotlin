package smithy.kotlin.codegen

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model

class KotlinCodegenPlugin : SmithyBuildPlugin {
    override fun getName(): String = "kotlin-codegen"

    override fun execute(context: PluginContext) {
        CodegenVisitor(context).execute()
    }
}