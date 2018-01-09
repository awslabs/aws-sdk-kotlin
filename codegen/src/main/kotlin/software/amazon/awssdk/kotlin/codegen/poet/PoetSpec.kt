package software.amazon.awssdk.kotlin.codegen.poet

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec

interface PoetSpec {
    val name: String
    fun appendTo(file: FileSpec.Builder)
}

abstract class ClassSpec(override val name: String) : PoetSpec {
    abstract fun typeSpec() : TypeSpec

    override final fun appendTo(file: FileSpec.Builder) {
        file.addType(typeSpec())
        appendHook(file)
    }

    open fun appendHook(file: FileSpec.Builder) {}
}
