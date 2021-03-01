package aws.sdk.kotlin.codegen.awsjson

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.ModeledExceptionsFeature
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.addImport
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator

class AwsJsonModeledExceptionsFeature(
    generationContext: ProtocolGenerator.GenerationContext,
    httpBindingResolver: HttpBindingResolver
) : ModeledExceptionsFeature(generationContext, httpBindingResolver) {
    override val name: String = "RestJsonError"

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        writer.addImport("RestJsonError", AwsKotlinDependency.REST_JSON_FEAT)
    }

    override fun renderRegisterErrors(writer: KotlinWriter) {
        val errors = getModeledErrors()

        errors.forEach { errShape ->
            val code = errShape.id.name
            val symbol = ctx.symbolProvider.toSymbol(errShape)
            val deserializerName = "${symbol.name}Deserializer"

            writer.write("register(code = #S, deserializer = $deserializerName())", code)
        }
    }
}
