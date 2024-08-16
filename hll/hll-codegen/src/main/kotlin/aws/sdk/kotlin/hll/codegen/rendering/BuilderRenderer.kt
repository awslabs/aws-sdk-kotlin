package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import com.google.devtools.ksp.symbol.*

/**
 * A DSL-style builder renderer.
 * @param renderer The base renderer in which the builder will be written
 * @param classDeclaration The [KSClassDeclaration] for which a builder will be generated
 */
class BuilderRenderer(
    private val renderer: RendererBase,
    private val classDeclaration: KSClassDeclaration,
) {
    private val properties = classDeclaration.getAllProperties().mapNotNull(KSClassProperty.Companion::from)

    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val classType: TypeRef = Type.from(classDeclaration)

    fun render() = renderer.apply {
        docs("A DSL-style builder for instances of [#T]", classType)

        withBlock("public class #L {", "}", "${className}Builder") {
            properties.forEach {
                write("public var #L: #T? = null", it.name, it.typeRef)
            }
            blankLine()

            withBlock("public fun build(): #T {", "}", classType) {
                properties.forEach {
                    if (it.nullable) {
                        write("val #1L = #1L", it.name)
                    } else {
                        write("val #1L = requireNotNull(#1L) { #2S }", it.name, "Missing value for ${it.name}")
                    }
                }
                blankLine()
                withBlock("return #T(", ")", classType) {
                    properties.forEach {
                        write("#L,", it.name)
                    }
                }
            }
        }
        blankLine()
    }
}

private data class KSClassProperty(val name: String, val typeRef: TypeRef, val typeName: KSName, val nullable: Boolean) {
    companion object {
        fun from(ksProperty: KSPropertyDeclaration): KSClassProperty? {
            val type: KSType = ksProperty.getter?.returnType?.resolve() ?: return null

            val name = ksProperty.simpleName.getShortName()
            val typeRef = Type.from(checkNotNull(ksProperty.type) { "Failed to determine class type for $name" })
            val typeName = type.declaration.qualifiedName ?: return null
            val nullable = type.nullability != Nullability.NOT_NULL

            return KSClassProperty(name, typeRef, typeName, nullable)
        }
    }
}
