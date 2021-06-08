/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.ErrorTrait
import software.amazon.smithy.model.traits.HttpErrorTrait

/**
 * Base class for registering modeled exceptions for HTTP protocols
 */
abstract class ModeledExceptionsMiddleware(
    protected val ctx: ProtocolGenerator.GenerationContext,
    protected val httpBindingResolver: HttpBindingResolver
) : ProtocolMiddleware {

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        writer.addImport("ExceptionRegistry", AwsKotlinDependency.AWS_CLIENT_RT_HTTP)
    }

    protected fun getModeledErrors(): Set<Shape> {
        val operations = httpBindingResolver.bindingOperations()
        return operations.flatMap { it.errors }.map { ctx.model.expectShape(it) }.toSet()
    }

    override fun renderProperties(writer: KotlinWriter) {
        writer.openBlock("private val exceptionRegistry = ExceptionRegistry().apply{")
            .call { renderRegisterErrors(writer) }
            .closeBlock("}")
    }

    /**
     * Register errors with the ExceptionRegistry
     */
    open fun renderRegisterErrors(writer: KotlinWriter) {
        val errors = getModeledErrors()

        errors.forEach { errShape ->
            val code = errShape.id.name
            val symbol = ctx.symbolProvider.toSymbol(errShape)
            val deserializerName = "${symbol.name}Deserializer"
            // If model specifies error code use it otherwise default to 400 (client) or 500 (server)
            // See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html#httperror-trait
            val defaultCode = if (errShape.expectTrait<ErrorTrait>().isClientError) 400 else 500
            val httpStatusCode = errShape.getTrait<HttpErrorTrait>()?.code ?: defaultCode
            writer.write("register(code = #S, deserializer = $deserializerName(), httpStatusCode = $httpStatusCode)", code)
        }
    }

    override fun renderConfigure(writer: KotlinWriter) {
        // wholesale override the registry rather than registering exceptions per/operation
        writer.write("registry = exceptionRegistry")
    }
}
