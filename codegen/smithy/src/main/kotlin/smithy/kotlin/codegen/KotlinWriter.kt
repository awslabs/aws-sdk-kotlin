package smithy.kotlin.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import software.amazon.smithy.utils.CodeWriter

class KotlinWriter(packageName: String, fileName: String) : CodeWriter() {
    // TODO package name
    private val fileBuilder = FileSpec.builder(packageName, fileName)

    fun addType(type: TypeSpec) {
        fileBuilder.addType(type)
    }

    override fun toString(): String {
        return buildString {
            fileBuilder.build().writeTo(this)
        }
    }
}
