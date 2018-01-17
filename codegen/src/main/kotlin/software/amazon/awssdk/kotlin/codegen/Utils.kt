package software.amazon.awssdk.kotlin.codegen

import software.amazon.awssdk.codegen.model.intermediate.MemberModel
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile


fun serviceModelInputStreamFromClass(clz: Class<*>): Pair<InputStream, InputStream?> {
    return loadServiceModelFromJar(File(clz.protectionDomain.codeSource.location.file))
}

fun loadServiceModelFromJar(jarFile: File): Pair<InputStream, InputStream?> {
    try {
        val jar = JarFile(jarFile)
        val customization = jar.getJarEntry("codegen-resources/customization.config")?.let { jar.getInputStream(it) }
        return jar.getInputStream(jar.getJarEntry("codegen-resources/service-2.json")) to customization
    } catch (e: Exception) {
        throw GenerationException("Unable to load service model from jar: $jarFile.", e)
    }
}

val String.isDependencyNotationWithoutVersion: Boolean get() = contains(":") && indexOf(":") == lastIndexOf(":")
val String.containsOnlyLettersAndDigits: Boolean get() = filter { it.isLetterOrDigit() }.length == length

val MemberModel.isSimpleScalarOrSimpleCollection: Boolean get() = this.isSimple || this.isSimpleCollection
val MemberModel.isSimpleCollection: Boolean get() = (this.isMap || this.isList)
        && !this.isCollectionWithBuilderMember
        && this.listModel?.listMemberModel?.enumType.isNullOrEmpty()

val MemberModel.isCollection: Boolean get() = this.isMap || this.isList