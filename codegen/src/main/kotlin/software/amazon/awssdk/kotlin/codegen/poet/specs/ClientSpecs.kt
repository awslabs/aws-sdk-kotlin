package software.amazon.awssdk.kotlin.codegen.poet.specs

import com.squareup.kotlinpoet.*
import software.amazon.awssdk.awscore.AwsRequest
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel
import software.amazon.awssdk.codegen.model.intermediate.OperationModel
import software.amazon.awssdk.core.ApiName
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.kotlin.codegen.CodeGenOptions
import software.amazon.awssdk.kotlin.codegen.NAME
import software.amazon.awssdk.kotlin.codegen.VERSION
import software.amazon.awssdk.kotlin.codegen.poet.ClassSpec
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions

class SyncClientSpec(private val model: IntermediateModel,
                     private val poetExtensions: PoetExtensions,
                     private val apiName: ApiName?,
                     val codeGenOptions: CodeGenOptions) : ClassSpec(model.metadata.syncClient) {
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
            .flatMap { operationSpec(it) }

    private fun operationSpec(model: OperationModel): Iterable<FunSpec> {
        return when {
            model.hasStreamingInput() && model.hasStreamingOutput() -> TODO("Streaming input/output operations not yet supported")
            model.hasStreamingInput() -> listOf(streamingInputOperationSpec(model))
            model.hasStreamingOutput() -> listOf(streamingOutputOperationSpec(model))
            model.inputShape?.members?.isEmpty() == false && codeGenOptions.builderSyntax -> listOf(basicOperationSpec(model), builderOverloadSpec(model))
            else -> listOf(basicOperationSpec(model))
        }
    }

    private fun builderOverloadSpec(model: OperationModel): FunSpec {
        val inputType = poetExtensions.modelClass(model.input.variableType)
        val builder = inputType.nestedClass("Builder")
        val block = LambdaTypeName.get(builder, returnType = Unit::class.asTypeName())

        return FunSpec.builder(model.methodName)
                .returns(poetExtensions.modelClass(model.returnType.returnType))
                .addParameter("block", block)
                .addCode("return %N(%T().apply(block).build())", model.methodName, builder)
                .addKdoc(documentation(model))
                .build()
    }

    private fun basicOperationSpec(model: OperationModel): FunSpec {
        return FunSpec.builder(model.methodName)
                .returns(poetExtensions.modelClass(model.returnType.returnType))
                .addParameter(model.input.variableName, poetExtensions.modelClass(model.input.variableType))
                .addCode("return %N.%L(%N.asJavaSdk().withUserAgent()).asKotlinSdk()", "client", model.methodName, model.input.variableName)
                .addKdoc(documentation(model))
                .build()
    }

    private fun streamingInputOperationSpec(model: OperationModel): FunSpec {
        return FunSpec.builder(model.methodName)
                .returns(poetExtensions.modelClass(model.returnType.returnType))
                .addParameter(model.input.variableName, poetExtensions.modelClass(model.input.variableType))
                .addParameter("body", RequestBody::class.asTypeName())
                .addCode("return %N.%L(%N.asJavaSdk().withUserAgent(), body).asKotlinSdk()", "client", model.methodName, model.input.variableName)
                .addKdoc(documentation(model))
                .build()
    }

    private fun streamingOutputOperationSpec(model: OperationModel): FunSpec {
        val parameterizedType = TypeVariableName("ReturnT")
        val transformer = ParameterizedTypeName.get(ResponseTransformer::class.asTypeName(),
                poetExtensions.modelClass(model.returnType.returnType),
                parameterizedType)

        val handlerWrapper = CodeBlock.of("{ response, stream -> transformer.transform(response.asKotlinSdk(), stream) }",
                ResponseTransformer::class.asTypeName()
            )

        return FunSpec.builder(model.methodName)
                .returns(parameterizedType)
                .addTypeVariable(parameterizedType)
                .addParameter(model.input.variableName, poetExtensions.modelClass(model.input.variableType))
                .addParameter("transformer", transformer)
                .addCode("return %N.%L(%N.asJavaSdk().withUserAgent()) %L", "client", model.methodName, model.input.variableName, handlerWrapper)
                .build()
    }

    private fun documentation(model: OperationModel): CodeBlock {
        return CodeBlock.of("See [%T.%N]\n", baseClass, model.methodName)
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
        val parameterizedType = TypeVariableName("T", AwsRequest::class)

        return FunSpec.builder("withUserAgent")
                .addModifiers(KModifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build())
                .addTypeVariable(parameterizedType)
                .receiver(parameterizedType)
                .returns(parameterizedType)
                .addCode("return this.toBuilder().overrideConfiguration { it.addApiName(apiName)")
                .apply {
                    if (apiName != null) {
                        this.addCode(".addApiName(pluginApiName)")
                    }
                }.addCode(" }.build() as T")
                .build()
    }

}