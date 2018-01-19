package software.amazon.awssdk.kotlin.codegen

import com.squareup.kotlinpoet.FileSpec
import software.amazon.awssdk.codegen.C2jModels
import software.amazon.awssdk.codegen.IntermediateModelBuilder
import software.amazon.awssdk.codegen.internal.Jackson
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeType
import software.amazon.awssdk.codegen.model.service.ServiceModel
import software.amazon.awssdk.core.ApiName
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions
import software.amazon.awssdk.kotlin.codegen.poet.PoetSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.EnumModelSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.ModelTransformerSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.ShapeModelSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.SyncClientSpec
import java.io.InputStream
import java.nio.file.Path

class CodeGenerator internal constructor(private val model: Model,
                                        private val outputDirectory: Path,
                                        targetBasePackage: String,
                                        private val codeGenOptions: CodeGenOptions,
                                        private val apiName: ApiName?) {
    private val intermediateModel = IntermediateModelBuilder(C2jModels.builder()
            .serviceModel(model.serviceModel)
            .customizationConfig(model.customizationConfig ?: CustomizationConfig.DEFAULT)
            .build()).build()
    private val packageForService = intermediateModel.metadata.fullClientPackageName.replace("software.amazon.awssdk", targetBasePackage)
    private val poetExtensions = PoetExtensions(packageForService, intermediateModel.metadata.fullClientPackageName)

    fun execute() {
        println("Generating AWS Kotlin SDK for ${model.serviceModel.name}")
        generateModelShapes(intermediateModel)
        generateTransformers(intermediateModel)
        generateClients(intermediateModel)
    }

    private fun generateModelShapes(intermediateModel: IntermediateModel) {
        val models = intermediateModel.kotlinSupportedShapes.map {
            convertToTypeSpec(it)
        }

        writeToFile(poetExtensions.modelPackage, "Models", models)
    }

    private fun generateTransformers(intermediateModel: IntermediateModel) {
        val transformers = intermediateModel.kotlinSupportedShapes.map {
            ModelTransformerSpec(it, poetExtensions)
        }

        writeToFile(poetExtensions.transformPackage, "Transformers", transformers)
    }

    private fun generateClients(intermediateModel: IntermediateModel) {
        FileSpec.builder(poetExtensions.basePackage, intermediateModel.metadata.syncInterface)
                .apply { SyncClientSpec(intermediateModel, poetExtensions, apiName, codeGenOptions).appendTo(this) }
                .build()
                .writeTo(outputDirectory)
    }

    private fun convertToTypeSpec(shapeModel: ShapeModel): PoetSpec {
        return if (shapeModel.shapeType == ShapeType.Enum) {
            EnumModelSpec(shapeModel, poetExtensions)
        } else {
            ShapeModelSpec(shapeModel, poetExtensions, codeGenOptions)
        }
    }

    private fun writeToFile(packageName: String, combinedFileName: String, specs: Iterable<PoetSpec>) {
        if (codeGenOptions.minimizeFiles) {
            FileSpec.builder(packageName, combinedFileName)
                    .apply { specs.forEach { it.appendTo(this) } }
                    .build()
                    .writeTo(outputDirectory)
        } else {
            specs.map {
                FileSpec.builder(packageName, it.name)
                        .apply { it.appendTo(this) }
                        .build()
            }.forEach { it.writeTo(outputDirectory) }
        }
    }

    private fun <T> loadModel(clz: Class<T>, inputStream: InputStream?): T? {
        if (inputStream != null) {
            return Jackson.load(clz, inputStream)
        }
        return null
    }

    private val IntermediateModel.kotlinSupportedShapes: List<ShapeModel>
        get() = this.shapes.values.filter { it.shapeType != ShapeType.Exception }

    private val ServiceModel.name: String get() = this.metadata.serviceFullName ?: this.metadata.serviceAbbreviation

}


data class CodeGenOptions internal constructor(internal val minimizeFiles: Boolean = false,
        //TODO: come up with a better name for this
                                               internal val builderSyntax: Boolean = true)


data class CodeGeneratorExecutor internal constructor(internal val outputDirectory: Path,
                                        internal val modelProvider: ModelProvider? = null,
                                        internal val targetBasePackage: String = "software.amazon.awssdk.kotlin",
                                        internal val codeGenOptions: CodeGenOptions = CodeGenOptions(),
                                        internal val apiName: ApiName? = null) {

    fun targetBasePackage(targetBasePackage: String): CodeGeneratorExecutor = copy(targetBasePackage = targetBasePackage)
    fun minimizeFiles(minimizeFiles: Boolean): CodeGeneratorExecutor = copy(codeGenOptions = codeGenOptions.copy(minimizeFiles = minimizeFiles))
    fun apiName(name: String, version: String): CodeGeneratorExecutor = copy(apiName = ApiName.builder().name(name).version(version).build())
    fun builderSyntax(builderSyntax: Boolean): CodeGeneratorExecutor = copy(codeGenOptions = codeGenOptions.copy(builderSyntax = builderSyntax))
    fun modelProvider(modelProvider: ModelProvider) = copy(modelProvider = modelProvider)
    fun modelProvider(modelProviders: Iterable<ModelProvider>) = copy(modelProvider = AggregateModelProvider(modelProviders))
    fun execute() {
        val provider = requireNotNull(modelProvider, {"modelProvider cannot be null"})
        provider.models().forEach {
            CodeGenerator(
                    model = it,
                    outputDirectory = outputDirectory,
                    targetBasePackage = targetBasePackage,
                    codeGenOptions = codeGenOptions,
                    apiName = apiName
            ).execute()
        }
    }

    companion object {
        fun builder(outputDirectory: Path): CodeGeneratorExecutor = CodeGeneratorExecutor(outputDirectory)
    }
}

