package smithy.kotlin.codegen.generators

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import smithy.kotlin.codegen.KotlinSettings
import smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.utils.StringUtils

class ServiceImplementationGenerator(
    private val settings: KotlinSettings,
    private val model: Model,
    private val symbolProvider: SymbolProvider,
    private val writer: KotlinWriter
) : Runnable {

    private val service = settings.getService(model)
    private val serviceSymbol = symbolProvider.toSymbol(service)

    override fun run() {
        val topDownIndex = model.getKnowledge(TopDownIndex::class.java)
        val baseName = serviceSymbol.name.removeSuffix("Client")
        val clientName = "${baseName}Client"
        val clientType = TypeSpec.classBuilder(clientName)
            .addSuperinterface(ClassName("", baseName))

        topDownIndex.getContainedOperations(service).sorted().forEach { operation ->
            val operationSymbol = symbolProvider.toSymbol(operation)

            val input = operationSymbol.expectProperty("inputType", Symbol::class.java)
            val output = operationSymbol.expectProperty("outputType", Symbol::class.java)

            val operationName = StringUtils.uncapitalize(operationSymbol.name)

            val inputType = ClassName(input.namespace, input.name)
            val outputType = ClassName(input.namespace, output.name)

            clientType.addFunction(
                FunSpec.builder(operationName)
                    .addModifiers(KModifier.SUSPEND, KModifier.OVERRIDE)
                    .addParameter(
                        ParameterSpec.builder(
                            "request",
                            LambdaTypeName.get(receiver = inputType, returnType = UNIT)
                        ).build()
                    )
                    .returns(outputType)
                    .build()
            )
        }

        writer.addType(clientType.build())
    }
}