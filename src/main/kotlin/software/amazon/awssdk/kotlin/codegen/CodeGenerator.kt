package software.amazon.awssdk.kotlin.codegen

import com.squareup.kotlinpoet.FileSpec
import software.amazon.awssdk.codegen.C2jModels
import software.amazon.awssdk.codegen.IntermediateModelBuilder
import software.amazon.awssdk.codegen.internal.Jackson
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig
import software.amazon.awssdk.codegen.model.service.ServiceModel
import software.amazon.awssdk.kotlin.codegen.poet.ShapeModelSpec
import software.amazon.awssdk.services.sts.STSClient

class CodeGenerator(private val models: C2jModels) {

    fun execute() {
        val intermediateModel = IntermediateModelBuilder(models).build()

        FileSpec.builder("", "Blah").addType(ShapeModelSpec(intermediateModel.shapes.values.first()).spec()).build().writeTo(System.out)
    }
}

fun loadServiceModel(clz: Class<*>): ServiceModel {
    return Jackson.load(ServiceModel::class.java, clz.getResourceAsStream("/codegen-resources/service-2.json"))
}

fun main(args: Array<String>) {
    val models = C2jModels.builder()
            .serviceModel(loadServiceModel(STSClient::class.java))
            .customizationConfig(CustomizationConfig.DEFAULT)
            .build()
    CodeGenerator(models).execute()
}
