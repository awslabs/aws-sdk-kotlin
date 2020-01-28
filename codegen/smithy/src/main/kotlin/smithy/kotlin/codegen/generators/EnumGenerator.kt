package smithy.kotlin.codegen.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Builder
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.model.shapes.StringShape
import software.amazon.smithy.model.traits.EnumTrait

class EnumGenerator(private val shape: StringShape, private val symbol: Symbol) {
    private val enumTrait = shape.getTrait(EnumTrait::class.java).get()

    fun run() = generateUnnamedEnum()

    private fun generateUnnamedEnum(): TypeSpec {
        val sortedEnums = enumTrait.values.entries.sortedBy { it.key }

        val enumBuilder = TypeSpec.enumBuilder(ClassName(symbol.namespace, symbol.name))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", STRING.copy(nullable = true))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("value", STRING.copy(nullable = true))
                    .initializer("value")
                    .build()
            )

        sortedEnums.forEach {
            val typeSpec = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%S", it.key)
            if (it.value.documentation.isPresent) {
                typeSpec.addKdoc(it.value.documentation.get())
            }

            enumBuilder.addEnumConstant(it.value.name.orElse(it.key), typeSpec.build())
        }

        addUnknownToSdk(enumBuilder)

        return enumBuilder.build()
    }

    private fun addUnknownToSdk(enumBuilder: Builder) {
        val typeSpec = TypeSpec.anonymousClassBuilder()
            .addSuperclassConstructorParameter("null")
            .addKdoc("Represents the enum value is unknown to this version")

        enumBuilder.addEnumConstant("UNKNOWN_TO_SDK_VERSION", typeSpec.build())
    }
}
