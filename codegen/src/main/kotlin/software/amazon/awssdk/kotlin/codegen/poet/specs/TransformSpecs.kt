package software.amazon.awssdk.kotlin.codegen.poet.specs

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.joinToCode
import software.amazon.awssdk.codegen.model.intermediate.MemberModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeType
import software.amazon.awssdk.kotlin.codegen.isSimpleScalarOrSimpleCollection
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions
import software.amazon.awssdk.kotlin.codegen.poet.PoetSpec

class ModelTransformerSpec(private val model: ShapeModel, private val poetExtensions: PoetExtensions) : PoetSpec {
    private val javaSdkClass = poetExtensions.javaSdkModelClass(model.variable.variableType)
    private val kotlinSdkClass = poetExtensions.modelClass(model.variable.variableType)
    override val name: String = model.shapeName

    override fun appendTo(file: FileSpec.Builder) {
        if (model.shapeType == ShapeType.Enum) return
        if (model.shapeType != ShapeType.Request) { file.addFunction(asKotlinSdkFunction()) }
        if (model.shapeType != ShapeType.Response) { file.addFunction(asJavaSdkFunction()) }
        file.addAliasedImport(kotlinSdkClass, "Kt${model.variable.variableType}")
    }

    private fun asKotlinSdkFunction(): FunSpec {
        return FunSpec.builder("asKotlinSdk")
                .addAnnotation(poetExtensions.generated)
                .returns(kotlinSdkClass)
                .receiver(javaSdkClass)
                .apply {

                    val codeBlock = CodeBlock.builder().add("return %T(", kotlinSdkClass)

                    val assignments = model.nonStreamingMembers.map {
                        when {
                            !it.enumType.isNullOrEmpty() -> javaToKtCodeBlockSimple(it)
                            it.isSimpleScalarOrSimpleCollection -> javaToKtCodeBlockSimple(it)
                            it.isList -> javaToKtCodeBlockList(it)
                            it.isMap -> javaToKtCodeBlockMap(it)
                            else -> javaToKtCodeBlockBuildable(it)
                        }
                    }

                    this.addCode(codeBlock.add(assignments.joinToCode(",", suffix = ")")).build())

                }.build()
    }

    private fun asJavaSdkFunction(): FunSpec {
        return FunSpec.builder("asJavaSdk")
                .addAnnotation(poetExtensions.generated)
                .returns(javaSdkClass)
                .receiver(kotlinSdkClass).apply {
                val codeBlock = CodeBlock.builder().add("return %T.builder()", javaSdkClass)

                model.nonStreamingMembers.map {
                    it.variable.variableName to
                            when {
                                !it.enumType.isNullOrEmpty() -> ktToJavaCodeBlockSimple(it)
                                it.isSimpleScalarOrSimpleCollection -> ktToJavaCodeBlockSimple(it)
                                it.isList -> ktToJavaCodeBlockList(it)
                                it.isMap -> ktToJavaCodeBlockMap(it)
                                else -> ktToJavaCodeBlockBuildable(it)
                            }
                }.forEach { entry ->
                    codeBlock.add(".apply { if (%N != null) { ", entry.first)
                            .add(entry.second)
                            .add("} }")
                }

                this.addCode(codeBlock.add(".build()").build())

        }.build()
    }

    private fun javaToKtCodeBlockBuildable(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L = %1L()?.asKotlinSdk()", memberModel.variable.variableName)
    }

    private fun javaToKtCodeBlockList(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L = %1L()?.map { it.asKotlinSdk() }", memberModel.variable.variableName)
    }

    private fun javaToKtCodeBlockMap(memberModel: MemberModel): CodeBlock {
        if (memberModel.mapModel?.keyModel?.enumType != null || !memberModel.mapModel.keyModel.isSimple) {
            TODO("Can't handle enums or complex types as keys yet got $memberModel")
        }
        return CodeBlock.of("%1L = %1L()?.mapValues { it.value.asKotlinSdk() }", memberModel.variable.variableName)
    }

    private fun javaToKtCodeBlockSimple(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L = %1L()", memberModel.variable.variableName)
    }

    private fun ktToJavaCodeBlockBuildable(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L(%1L.asJavaSdk())", memberModel.variable.variableName)
    }

    private fun ktToJavaCodeBlockList(memberModel: MemberModel): CodeBlock {
        val listModel = memberModel.listModel.listMemberModel
        if (!listModel.isList && !listModel.isMap) {
            return CodeBlock.of("%1L(%1L.map { it.%2L })",
                    memberModel.variable.variableName,
                    if (listModel.enumType.isNullOrEmpty()) "asJavaSdk()" else "name")
        } else {
            TODO("Nested list of collections")
        }
    }

    private fun ktToJavaCodeBlockMap(memberModel: MemberModel): CodeBlock {
        if (memberModel.mapModel?.keyModel?.enumType != null || !memberModel.mapModel.keyModel.isSimple) {
            TODO("Can't handle enums or complex types as keys yet got $memberModel")
        }
        return CodeBlock.of("%1L(%1L.mapValues { it.value.asJavaSdk() })", memberModel.variable.variableName)
    }

    private fun ktToJavaCodeBlockSimple(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L(%1L)", memberModel.variable.variableName)
    }
}


