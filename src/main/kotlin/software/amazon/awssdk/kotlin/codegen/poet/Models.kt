package software.amazon.awssdk.kotlin.codegen.poet

import com.squareup.kotlinpoet.*
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel
import software.amazon.awssdk.kotlin.codegen.GenerationException

class ShapeModelSpec(private val model: ShapeModel) {

    fun spec(): TypeSpec {

        val params = parameters();

        return TypeSpec.classBuilder(model.shapeName)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameters(params)
                        .build())
                .addProperties(params.map { PropertySpec.builder(it.name, it.type).initializer(it.name).build() })
                .build()
    }

    private fun parameters(): Iterable<ParameterSpec> {
        return model.nonStreamingMembers.map {
            ParameterSpec.builder(it.variable.variableName,
                    Utils.simpleTypeMap.getOrElse(it.variable.variableType, { throw GenerationException("Unknown type ${it.variable.variableType}") }).asNullable())
                    .defaultValue("null")
                    .build()
        }
    }
}