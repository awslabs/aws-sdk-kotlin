/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.addImport
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.shapes.Shape

/**
 * Base class for registering modeled exceptions for HTTP protocols
 */
abstract class ModeledExceptionsFeature(
    protected val ctx: ProtocolGenerator.GenerationContext,
    protected val httpBindingResolver: HttpBindingResolver
) : HttpFeature {

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
    abstract fun renderRegisterErrors(writer: KotlinWriter)

    override fun renderConfigure(writer: KotlinWriter) {
        // wholesale override the registry rather than registering exceptions per/operation
        writer.write("registry = exceptionRegistry")
    }
}
