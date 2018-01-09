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
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions
import software.amazon.awssdk.kotlin.codegen.poet.PoetSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.EnumModelSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.ModelTransformerSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.ShapeModelSpec
import software.amazon.awssdk.kotlin.codegen.poet.specs.SyncClientSpec
import java.io.InputStream
import java.nio.file.Path

class CodeGenerator private constructor(private val params: Builder) {
    private val serviceModel = loadModel(ServiceModel::class.java, params.serviceModel)!!
    private val customization = loadModel(CustomizationConfig::class.java,params.customizationConfig) ?: CustomizationConfig.DEFAULT
    private val intermediateModel = IntermediateModelBuilder(C2jModels.builder()
            .serviceModel(serviceModel)
            .customizationConfig(customization)
            .build()).build()
    private val packageForService = intermediateModel.metadata.fullClientPackageName.replace("software.amazon.awssdk", params.targetPackageBase)
    private val poetExtensions = PoetExtensions(packageForService, intermediateModel.metadata.fullClientPackageName)

    fun execute() {
        println("Generating AWS Kotlin SDK for ${serviceModel.name}")
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
        FileSpec.builder(poetExtensions.basePackage, intermediateModel.metadata.syncClient)
                .apply { SyncClientSpec(intermediateModel, poetExtensions).appendTo(this) }
                .build()
                .writeTo(params.outputDirectory)
    }

    private fun convertToTypeSpec(shapeModel: ShapeModel): PoetSpec {
        return if (shapeModel.shapeType == ShapeType.Enum) {
            EnumModelSpec(shapeModel)
        } else {
            ShapeModelSpec(shapeModel, poetExtensions)
        }
    }

    private fun writeToFile(packageName: String, combinedFileName: String, specs: Iterable<PoetSpec>) {
        if (params.minimizeFiles) {
            FileSpec.builder(packageName, combinedFileName)
                    .apply { specs.forEach { it.appendTo(this) } }
                    .build()
                    .writeTo(params.outputDirectory)
        } else {
            specs.map {
                FileSpec.builder(packageName, it.name)
                        .apply { it.appendTo(this) }
                        .build()
            }.forEach { it.writeTo(params.outputDirectory) }
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

    data class Builder internal constructor(internal val outputDirectory: Path,
                                            internal val serviceModel: InputStream? = null,
                                            internal val customizationConfig: InputStream? = null,
                                            internal val targetPackageBase: String = "software.amazon.awssdk.kotlin",
                                            internal val minimizeFiles: Boolean = false) {

        fun targetPackageBase(targetPackageBase: String): Builder = copy(targetPackageBase = targetPackageBase)
        fun minimizeFiles(minimizeFiles: Boolean): Builder = copy(minimizeFiles = minimizeFiles)
        fun serviceModel(serviceModel: InputStream): Builder = copy(serviceModel = serviceModel)
        fun customizationConfig(customizationConfig: InputStream): Builder = copy(customizationConfig = customizationConfig)
        fun build(): CodeGenerator = CodeGenerator(this)
    }

    companion object {
        fun builder(outputDirectory: Path): Builder = Builder(outputDirectory)
    }
}

