package aws.sdk.kotlin.hll.codegen.rendering

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * A DSL-style builder renderer.
 * @param renderer The base renderer in which the builder will be written
 * @param classDeclaration The [KSClassDeclaration] for which a builder will be generated
 */
class BuilderRenderer(
    private val renderer: RendererBase,
    classDeclaration: KSClassDeclaration,
) {
    private val properties = classDeclaration.getAllProperties().mapNotNull(KSClassProperty.Companion::from)

    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val builderName = "${className}Builder"

    fun render() {
        renderer.withDocs {
            renderer.write("A DSL-style builder for instances of [#L]", className)
        }

        renderer.withBlock("public class #L {", "}", builderName) {
            properties.forEach {
                renderer.write("public var #L: #L? = null", it.name, it.typeName.asString())
            }
            renderer.blankLine()

            renderer.withBlock("public fun build(): #L {", "}", className) {
                properties.forEach {
                    renderer.write("val #1L = requireNotNull(#1L) { #2S }", it.name, "Missing value for ${it.name}")
                }
                renderer.blankLine()
                renderer.withBlock("return #L(", ")", className) {
                    properties.forEach {
                        renderer.write("#L,", it.name)
                    }
                }
            }
        }
        renderer.blankLine()
    }
}

private data class KSClassProperty(val name: String, val typeName: KSName) {
    companion object {
        fun from(ksProperty: KSPropertyDeclaration) = ksProperty
            .getter
            ?.returnType
            ?.resolve()
            ?.declaration
            ?.qualifiedName
            ?.let { typeName -> KSClassProperty(ksProperty.simpleName.getShortName(), typeName) }
    }
}