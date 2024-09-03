package aws.sdk.kotlin.hll.codegen.rendering

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.smithy.kotlin.runtime.collections.get
import aws.sdk.kotlin.hll.codegen.rendering.RenderOptions

/**
 * A DSL-style builder renderer.
 * @param renderer The base renderer in which the builder will be written
 * @param classType The [TypeRef] representing the class for which a builder will be generated
 * @param members The [Set] of members of [classType] which will be included in the builder
 */
class BuilderRenderer(
    private val renderer: RendererBase,
    private val classType: TypeRef,
    private val members: Set<Member>,
    private val ctx: RenderContext,
) {
    private val className = classType.shortName

    fun render() = renderer.apply {
        docs("A DSL-style builder for instances of [#T]", classType)

        withBlock("#Lclass #L {", "}", ctx.attributes.visibility, "${className}Builder") {
            members.forEach {
                write("#Lvar #L: #T? = null", ctx.attributes.visibility, it.name, it.type)
            }
            blankLine()

            withBlock("#Lfun build(): #T {", "}", ctx.attributes.visibility, classType) {
                members.forEach {
                    if (it.type.nullable) {
                        write("val #1L = #1L", it.name)
                    } else {
                        write("val #1L = requireNotNull(#1L) { #2S }", it.name, "Missing value for ${it.name}")
                    }
                }
                blankLine()
                withBlock("return #T(", ")", classType) {
                    members.forEach {
                        write("#L,", it.name)
                    }
                }
            }
        }
        blankLine()
    }
}
