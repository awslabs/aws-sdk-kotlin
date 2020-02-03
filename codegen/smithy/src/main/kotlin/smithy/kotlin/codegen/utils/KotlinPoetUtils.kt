package smithy.kotlin.codegen.utils

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.DocumentationTrait

fun TypeSpec.Builder.addKdoc(shape: Shape): TypeSpec.Builder {
    shape.getTrait(DocumentationTrait::class.java)
        .map { it.value }
        .ifPresent { addKdoc(it) }
    return this
}

fun PropertySpec.Builder.addKdoc(shape: Shape): PropertySpec.Builder {
    shape.getTrait(DocumentationTrait::class.java)
        .map { it.value }
        .ifPresent { addKdoc(it) }
    return this
}