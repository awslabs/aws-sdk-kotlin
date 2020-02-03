package smithy.kotlin.codegen.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import smithy.kotlin.codegen.KotlinWriter
import smithy.kotlin.codegen.utils.addKdoc
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.IdempotencyTokenTrait
import software.amazon.smithy.utils.StringUtils

class StructureGenerator(
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val shape: StructureShape,
    private val writer: KotlinWriter
) {
    fun generate() {
        writer.addType(
            if (!shape.hasTrait(ErrorTrait::class.java)) {
                generateNonErrorStructure()
            } else {
                generateErrorStructure()
            }
        )
    }

    private fun generateNonErrorStructure(): TypeSpec {
        val symbol = symbolProvider.toSymbol(shape)

        return TypeSpec.classBuilder(ClassName(symbol.namespace, symbol.name))
            .addKdoc(shape)
            .addMembers()
            .build()
    }

    private fun generateErrorStructure(): TypeSpec {
        val errorTrait = shape.getTrait(ErrorTrait::class.java).orElseThrow { IllegalStateException() }
        val symbol = symbolProvider.toSymbol(shape)

        return TypeSpec.classBuilder(ClassName(symbol.namespace, symbol.name))
            .addKdoc(shape)
            .addMembers()
            .build()
    }

    private fun TypeSpec.Builder.addMembers(): TypeSpec.Builder {
        val members = shape.allMembers.values

        members.forEach { member ->
            val isNullable = isNullableMember(member)
            val typeName = createTypeName(member).copy(nullable = isNullable)
            val propertyName = StringUtils.uncapitalize(member.memberName)
            this.addProperty(
                PropertySpec.builder(propertyName, typeName)
                    .addKdoc(member)
                    .build()
            )
        }

        return this;
    }

    private fun createTypeName(member: Shape): TypeName {
        val symbol = symbolProvider.toSymbol(member)
        val targetShape = symbol.expectProperty("shape", Shape::class.java)
        return when {
            targetShape.isListShape -> {
                val genericSymbol = symbol.references.first().symbol
                LIST.parameterizedBy(ClassName(genericSymbol.namespace, genericSymbol.name))
            }
            else -> ClassName(symbol.namespace, symbol.name)
        }
    }

    private fun isNullableMember(member: MemberShape): Boolean {
        return !member.isRequired || member.hasTrait(IdempotencyTokenTrait::class.java)
    }
}


