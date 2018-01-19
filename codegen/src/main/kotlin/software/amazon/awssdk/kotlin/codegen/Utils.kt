package software.amazon.awssdk.kotlin.codegen

import software.amazon.awssdk.codegen.model.intermediate.MemberModel
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

val String.isDependencyNotationWithoutVersion: Boolean get() = contains(":") && indexOf(":") == lastIndexOf(":")
val String.containsOnlyLettersAndDigits: Boolean get() = filter { it.isLetterOrDigit() }.length == length

val MemberModel.isSimpleScalarOrSimpleCollection: Boolean get() = this.isSimple || this.isSimpleCollection
val MemberModel.isSimpleCollection: Boolean get() = (this.isMap || this.isList)
        && !this.isCollectionWithBuilderMember
        && this.listModel?.listMemberModel?.enumType.isNullOrEmpty()

val MemberModel.isCollection: Boolean get() = this.isMap || this.isList