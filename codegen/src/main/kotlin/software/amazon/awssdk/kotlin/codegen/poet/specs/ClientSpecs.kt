package software.amazon.awssdk.kotlin.codegen.poet.specs

import com.squareup.kotlinpoet.*
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel
import software.amazon.awssdk.codegen.model.intermediate.OperationModel
import software.amazon.awssdk.core.ApiName
import software.amazon.awssdk.core.AwsRequest
import software.amazon.awssdk.core.AwsRequestOverrideConfig
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.StreamingResponseHandler
import software.amazon.awssdk.http.AbortableInputStream
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
        val handler = ParameterizedTypeName.get(StreamingResponseHandler::class.asTypeName(),
                poetExtensions.modelClass(model.returnType.returnType),
                parameterizedType)

        val javaSdkHandler = ParameterizedTypeName.get(StreamingResponseHandler::class.asTypeName(), poetExtensions.javaSdkModelClass(model.returnType.returnType), parameterizedType)
        val inputStreamType = AbortableInputStream::class.asTypeName().asNullable()

        val handlerWrapper = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(javaSdkHandler)
                .addFunction(FunSpec.builder("apply")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(parameterizedType)
                        .addParameter("javaResponse", poetExtensions.javaSdkModelClass(model.returnType.returnType))
                        .addParameter("stream", inputStreamType)
                        .addCode("return handler.apply(javaResponse.asKotlinSdk(), stream)")
                        .build())
                .addFunction(FunSpec.builder("needsConnectionLeftOpen")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Boolean::class)
                        .addCode("return handler.needsConnectionLeftOpen()")
                        .build())
                .build().toString().replace("ReturnT>()", "ReturnT>") //HACK: KotlinPoet doesn't properly handle interfaces on anonymous objects

        return FunSpec.builder(model.methodName)
                .returns(parameterizedType)
                .addTypeVariable(parameterizedType)
                .addParameter(model.input.variableName, poetExtensions.modelClass(model.input.variableType))
                .addParameter("handler", handler)
                .addCode("return %N.%L(%N.asJavaSdk().withUserAgent(), %L)", "client", model.methodName, model.input.variableName, handlerWrapper)
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