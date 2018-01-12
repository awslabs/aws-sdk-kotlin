package software.amazon.awssdk.kotlin.codegen.poet.specs

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import software.amazon.awssdk.codegen.model.intermediate.MemberModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel
import software.amazon.awssdk.codegen.model.intermediate.ShapeType
import software.amazon.awssdk.kotlin.codegen.poet.PoetExtensions
import software.amazon.awssdk.kotlin.codegen.poet.PoetSpec

class ModelTransformerSpec(private val model: ShapeModel, private val poetExtensions: PoetExtensions) : PoetSpec {
    private val javaSdkClass = poetExtensions.javaSdkModelClass(model.variable.variableType)
    private val kotlinSdkClass = poetExtensions.modelClass(model.variable.variableType)
    override val name: String = model.shapeName

    override fun appendTo(file: FileSpec.Builder) {
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
                    if (model.shapeType == ShapeType.Enum) {
                        this.addCode("return %T.valueOf(name)", kotlinSdkClass)
                    } else {
                        val codeBlock = CodeBlock.builder().add("return %T(", kotlinSdkClass)

                        val assignments = model.nonStreamingMembers.map {
                            when {
                                !it.enumType.isNullOrEmpty() -> javaToKtCodeBlockBuildable(it)
                                it.isSimpleScalarOrSimpleCollection -> javaToKtCodeBlockSimple(it)
                                it.isList -> javaToKtCodeBlockList(it)
                                it.isMap -> TODO("implement complex maps")
                                else -> javaToKtCodeBlockBuildable(it)
                            }
                        }

                        if (assignments.size > 1) {
                            assignments.dropLast(1).forEach { codeBlock.add(it).add(",") }
                        }
                        if (assignments.isNotEmpty()) {
                            codeBlock.add(assignments.last())
                        }

                        this.addCode(codeBlock.add(")").build())
                    }
                }.build()
    }

    private fun asJavaSdkFunction(): FunSpec {
        return FunSpec.builder("asJavaSdk")
                .addAnnotation(poetExtensions.generated)
                .returns(javaSdkClass)
                .receiver(kotlinSdkClass).apply {
            if (model.shapeType == ShapeType.Enum) {
                this.addCode("return %T.valueOf(name)", javaSdkClass)
            } else {
                val codeBlock = CodeBlock.builder().add("return %T.builder()", javaSdkClass)

                model.nonStreamingMembers.map {
                    it.variable.variableName to
                            when {
                                !it.enumType.isNullOrEmpty() -> ktToJavaCodeBlockBuildable(it)
                                it.isSimpleScalarOrSimpleCollection -> ktToJavaCodeBlockSimple(it)
                                it.isList -> ktToJavaCodeBlockList(it)
                                it.isMap -> TODO("implement complex maps")
                                else -> ktToJavaCodeBlockBuildable(it)
                            }
                }.forEach { entry ->
                    codeBlock.add(".apply { if (%N != null) { it.", entry.first)
                            .add(entry.second)
                            .add("} }")
                }

                this.addCode(codeBlock.add(".build()").build())
            }
        }.build()
    }

    private fun javaToKtCodeBlockBuildable(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L = %1L()?.asKotlinSdk()", memberModel.variable.variableName)
    }

    private fun javaToKtCodeBlockList(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L = %1L()?.map { it.asKotlinSdk() }", memberModel.variable.variableName)
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

    private fun ktToJavaCodeBlockSimple(memberModel: MemberModel): CodeBlock {
        return CodeBlock.of("%1L(%1L)", memberModel.variable.variableName)
    }
}

private val MemberModel.isSimpleScalarOrSimpleCollection: Boolean get() = this.isSimple || this.isSimpleCollection
private val MemberModel.isSimpleCollection: Boolean get() = (this.isMap || this.isList)
        && !this.isCollectionWithBuilderMember
        && this.listModel?.listMemberModel?.enumType.isNullOrEmpty()
