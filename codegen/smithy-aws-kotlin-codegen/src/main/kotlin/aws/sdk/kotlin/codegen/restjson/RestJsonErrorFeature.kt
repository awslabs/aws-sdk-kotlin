package aws.sdk.kotlin.codegen.restjson

import aws.sdk.kotlin.codegen.ModeledExceptionsFeature
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.traits.HttpErrorTrait

class RestJsonErrorFeature(
    ctx: ProtocolGenerator.GenerationContext,
    httpBindingResolver: HttpBindingResolver
) : ModeledExceptionsFeature(ctx, httpBindingResolver) {
    override val name: String = "RestJsonError"

    override fun renderConfigure(writer: KotlinWriter) {
        val errors = getModeledErrors()

        errors.forEach { errShape ->
            val code = errShape.id.name
            val symbol = ctx.symbolProvider.toSymbol(errShape)
            val deserializerName = "${symbol.name}Deserializer"
            val httpStatusCode: Int? = errShape.getTrait(HttpErrorTrait::class.java).map { it.code }.orElse(null)
            if (httpStatusCode != null) {
                writer.write("register(code = #S, deserializer = $deserializerName(), httpStatusCode = $httpStatusCode)", code)
            }
        }
    }
}
