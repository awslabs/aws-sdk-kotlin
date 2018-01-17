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
                     private val poetExtensions: PoetExtensions,
                     private val apiName: ApiName?) : ClassSpec(model.metadata.syncClient) {
    private val baseClass = poetExtensions.javaSdkClientClass(model.metadata.syncInterface)
    override fun typeSpec(): TypeSpec {
        return TypeSpec.classBuilder(model.metadata.syncInterface)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter(ParameterSpec.builder("client", baseClass)
                                .defaultValue(CodeBlock.of("%T.create()", baseClass))
                                .addModifiers(KModifier.PRIVATE)
                                .build())
                        .build())
                .addFunction(builderConstructor())
                .addProperty(PropertySpec.builder("client", baseClass)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("client").build())
                .addProperty(userAgent())
                .apply {
                    if (apiName != null) {
                        this.addProperty(userAgent(apiName))
                    }
                }
                .addAnnotation(poetExtensions.generated)
                .addFunctions(operationSpecs())
                .addFunction(userAgentExtension())
                .build()
    }

    private fun builderConstructor(): FunSpec {
        val javaSdkBuilder = poetExtensions.javaSdkClientClass(model.metadata.syncBuilderInterface)
        val block = LambdaTypeName.get(javaSdkBuilder, returnType = Unit::class.asTypeName())
        return FunSpec.constructorBuilder()
                .addParameter("block", block)
                .callThisConstructor(CodeBlock.of("client = %T.builder().apply(block).build()", baseClass))
                .build()
    }

    override fun appendHook(file: FileSpec.Builder) {
        file.addStaticImport(poetExtensions.transformPackage, "*")
        file.addAliasedImport(baseClass, "JavaSdk${model.metadata.syncInterface}")
    }

    private fun operationSpecs() = model.operations.values
            .filterNotNull()
            .filterNot { it.isStreaming }
            .flatMap { operationSpec(it) }

    private fun operationSpec(model: OperationModel): Iterable<FunSpec> {
        val basic = basicOperationSpec(model)
        if (model.inputShape?.nonStreamingMembers?.isNotEmpty() == true) {
            return listOf(basic, builderOverloadSpec(model))
        }
        return listOf(basic)
    }

    private fun builderOverloadSpec(model: OperationModel): FunSpec {
        val inputType = poetExtensions.modelClass(model.input.variableType)
        val builder = inputType.nestedClass("Builder")
        val block = LambdaTypeName.get(builder, returnType = Unit::class.asTypeName())

        return FunSpec.builder(model.methodName)
                .returns(poetExtensions.modelClass(model.returnType.returnType))
                .addParameter("block", block)
                .addCode("return %N(%T().apply(block).build())", model.methodName, builder)
                .build()
    }

    private fun funcs(blck: TypeSpec.Builder.() -> Unit) {

    }

    private fun basicOperationSpec(model: OperationModel): FunSpec {
        return FunSpec.builder(model.methodName)
                .returns(poetExtensions.modelClass(model.returnType.returnType))
                .addParameter(model.input.variableName, poetExtensions.modelClass(model.input.variableType))
                .addCode("return %N.%L(%N.asJavaSdk().withUserAgent()).asKotlinSdk()", "client", model.methodName, model.input.variableName)
                .build()
    }

    private fun userAgent(): PropertySpec {
        return PropertySpec.builder("apiName", ApiName::class.asClassName(), KModifier.PRIVATE)
                .initializer("%T.builder().name(%S).version(%S).build()", ApiName::class, NAME, VERSION).build()
    }

    private fun userAgent(apiName: ApiName): PropertySpec {
        return PropertySpec.builder("pluginApiName", ApiName::class.asClassName(), KModifier.PRIVATE)
                .initializer("%T.builder().name(%S).version(%S).build()", ApiName::class, apiName.name(), apiName.version()).build()
    }

    private fun userAgentExtension(): FunSpec {
        val parameterizedType = TypeVariableName.invoke("T", AwsRequest::class)

        return FunSpec.builder("withUserAgent")
                .addModifiers(KModifier.PRIVATE)
                .addTypeVariable(parameterizedType)
                .receiver(parameterizedType)
                .returns(parameterizedType)
                .addCode("val %L = this.requestOverrideConfig().map { it.toBuilder() }.orElseGet { %T.builder() }.addApiName(apiName)",
                        "cfg",
                        AwsRequestOverrideConfig::class)
                .apply {
                    if (apiName != null) {
                        this.addCode(".addApiName(pluginApiName)")
                    }
                }.addCode(".build()")
                .addCode("\n@%T(%S)\nreturn this.toBuilder().requestOverrideConfig(%L).build() as T", Suppress::class, "UNCHECKED_CAST", "cfg")
                .build()
    }

}