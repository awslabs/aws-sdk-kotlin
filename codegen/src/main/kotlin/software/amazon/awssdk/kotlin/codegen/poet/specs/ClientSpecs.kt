package software.amazon.awssdk.kotlin.codegen.poet.specs

import com.squareup.kotlinpoet.*
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel
import software.amazon.awssdk.codegen.model.intermediate.OperationModel
import software.amazon.awssdk.kotlin.codegen.poet.ClassSpec
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions
import software.amazon.awssdk.core.ApiName
import software.amazon.awssdk.core.AwsRequest
import software.amazon.awssdk.core.AwsRequestOverrideConfig
import software.amazon.awssdk.kotlin.codegen.NAME
import software.amazon.awssdk.kotlin.codegen.VERSION

class SyncClientSpec(private val model: IntermediateModel,
                     private val poetExtensions: PoetExtensions) : ClassSpec(model.metadata.syncClient) {
    private val baseClass = poetExtensions.javaSdkClientClass(model.metadata.syncInterface)
    override fun typeSpec(): TypeSpec {
        return TypeSpec.classBuilder(model.metadata.syncClient)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter(ParameterSpec.builder("client", baseClass)
                                .defaultValue(CodeBlock.of("%T.create()", baseClass))
                                .addModifiers(KModifier.PRIVATE)
                                .build())
                        .build())
                .addProperty(PropertySpec.builder("client", baseClass)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("client").build())
                .addProperty(userAgent())
                .addFunctions(functionSpecs())
                .addFunction(userAgentExtension())
                .build()
    }

    override fun appendHook(file: FileSpec.Builder) {
        file.addStaticImport(poetExtensions.transformPackage, "*")
    }

    private fun functionSpecs() = model.operations.values
            .filterNotNull()
            .filterNot { it.isStreaming }
            .map { functionSpec(it) }

    private fun functionSpec(model: OperationModel): FunSpec {
        return FunSpec.builder(model.methodName)
                .addParameter(model.input.variableName, poetExtensions.modelClass(model.input.variableType))
                .returns(poetExtensions.modelClass(model.returnType.returnType))
                .addCode("return %N.%L(%N.asJavaSdk().withUserAgent()).asKotlinSdk()", "client", model.methodName, model.input.variableName)
                .build()
    }

    private fun userAgent(): PropertySpec {
        return PropertySpec.builder("apiName", ApiName::class.asClassName(), KModifier.PRIVATE)
                .initializer("%T.builder().name(%S).version(%S).build()", ApiName::class, NAME, VERSION).build()
    }

    private fun userAgentExtension(): FunSpec {
        val parameterizedType = TypeVariableName.invoke("T", AwsRequest::class)

        return FunSpec.builder("withUserAgent")
                .addModifiers(KModifier.PRIVATE)
                .addTypeVariable(parameterizedType)
                .receiver(parameterizedType)
                .returns(parameterizedType)
                .addCode("val %L = this.requestOverrideConfig().map { it.toBuilder() }.orElseGet { %T.builder() }.addApiName(apiName).build()",
                        "cfg",
                        AwsRequestOverrideConfig::class)
                .addCode("\n@%T(%S)\nreturn this.toBuilder().requestOverrideConfig(%L).build() as T", Suppress::class, "UNCHECKED_CAST", "cfg")
                .build()
    }

}