package software.amazon.awssdk.kotlin.codegen.poet.specs

import software.amazon.awssdk.kotlin.codegen.poet.ClassSpec
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions.Companion.classNameForType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.DATA
import software.amazon.awssdk.codegen.model.intermediate.MemberModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel
import software.amazon.awssdk.kotlin.codegen.isCollection


class ShapeModelSpec(private val model: ShapeModel, private val poetExtensions: PoetExtensions) : ClassSpec(model.shapeName) {

    override fun typeSpec(): TypeSpec {

        val params = parameters()

        val className = poetExtensions.modelClass(model.shapeName)
        return TypeSpec.classBuilder(className)
                .apply {
                    if (params.isNotEmpty()) {
                        this.addModifiers(DATA)
                                .primaryConstructor(FunSpec.constructorBuilder()
                                        .addParameters(params)
                                        .build())
                                .addProperties(params.map { PropertySpec.builder(it.name, it.type).initializer(it.name).build() })
                                .addType(TypeSpec.classBuilder("Builder")
                                        .addModifiers(DATA)
                                        .primaryConstructor(FunSpec.constructorBuilder().addParameters(params).build())
                                        .addProperties(params.map { PropertySpec.builder(it.name, it.type).initializer(it.name).mutable(true).build() })
                                        .addFunction(builderBuildMethod(params, className))
                                        .addFunctions(complexBuilderSetters(model.nonStreamingMembers.filterNotNull()))
                                        .build())
                    }
                }
                .addAnnotation(poetExtensions.generated)
                .build()
    }

    private fun builderBuildMethod(params: List<ParameterSpec>, className: ClassName): FunSpec {

        val assignments = params.map { CodeBlock.of("%1N = %1N", it.name) }

        return FunSpec.builder("build")
                .returns(className)
                .addCode(CodeBlock.builder().add("return %T(", className).apply {
                    if (assignments.size > 1) {
                        assignments.dropLast(1).forEach { this.add(it).add(",") }
                    }
                    if (assignments.isNotEmpty()) {
                        this.add(assignments.last())
                    }
                }.add(")").build()).build()
    }

    private fun complexBuilderSetters(params: List<MemberModel>): Iterable<FunSpec> {
        return params.filterNot { it.isCollection || it.isSimple }.filter { it.variable != null }.map {
            val builder = poetExtensions.modelClass(it.variable.variableType).nestedClass("Builder")
            val block = LambdaTypeName.get(builder, returnType = Unit::class.asTypeName())

            FunSpec.builder(it.variable.variableName)
                    .addParameter("block", block)
                    .addCode("%N = %T().apply(block).build()", it.variable.variableName, builder)
                    .build()
        }
    }

    private fun parameters(): List<ParameterSpec> {

        return model.nonStreamingMembers.filterNotNull()
                .map {
                    val typeName = determineTypeName(it)
                    ParameterSpec.builder(it.variable.variableName, typeName.asNullable())
                            .defaultValue("null")
                            .build()
                }
    }

    private fun determineTypeName(memberModel: MemberModel): TypeName {
        if (!memberModel.enumType.isNullOrEmpty()) {
            return poetExtensions.modelClass(memberModel.enumType)
        }

        if (memberModel.isSimple) {
            return classNameForType(memberModel.variable.variableType)
        }

        if (memberModel.isList) {
            return ParameterizedTypeName.get(List::class.asClassName(), determineTypeName(memberModel.listModel.listMemberModel))
        }

        if (memberModel.isMap) {
            return ParameterizedTypeName.get(Map::class.asClassName(),
                    determineTypeName(memberModel.mapModel.keyModel),
                    determineTypeName(memberModel.mapModel.valueModel))
        }
        return poetExtensions.modelClass(memberModel.variable.variableType)
    }
}

class EnumModelSpec(private val model: ShapeModel) : ClassSpec(model.shapeName) {
    override fun typeSpec(): TypeSpec {
        val builder = TypeSpec.enumBuilder(model.shapeName)
        model.enums.forEach { builder.addEnumConstant(it.name) }
        builder.addEnumConstant("SDK_UNKNOWN")
        return builder.build()
    }

}