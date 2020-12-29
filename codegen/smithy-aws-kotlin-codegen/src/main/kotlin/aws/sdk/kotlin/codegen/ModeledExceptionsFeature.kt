/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinWriter
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
        val restJsonSymbol = Symbol.builder()
            .name("RestJsonError")
            .namespace(AwsKotlinDependency.REST_JSON_FEAT.namespace, ".")
            .addDependency(AwsKotlinDependency.REST_JSON_FEAT)
            .build()
        writer.addImport(restJsonSymbol, "")
    }

    protected fun getModeledErrors(): Set<Shape> {
        val operations = httpBindingResolver.bindingOperations()

        return operations.flatMap { it.errors }.map { ctx.model.expectShape(it) }.toSet()
    }

    abstract override fun renderConfigure(writer: KotlinWriter)
}
