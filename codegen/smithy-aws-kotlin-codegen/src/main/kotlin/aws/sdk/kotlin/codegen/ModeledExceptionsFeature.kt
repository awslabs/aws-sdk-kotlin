/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.Shape
import software.amazon.smithy.model.traits.HttpErrorTrait
import software.amazon.smithy.model.traits.HttpTrait

/**
 * Base class for registering modeled exceptions for HTTP protocols
 */
abstract class ModeledExceptionsFeature(protected val ctx: ProtocolGenerator.GenerationContext) : HttpFeature {
    private fun getModeledErrors(): Set<Shape> {
        val topDownIndex: TopDownIndex = TopDownIndex.of(ctx.model)
        val operations = topDownIndex.getContainedOperations(ctx.service)
            .filter { it.hasTrait(HttpTrait::class.java) }.toList<OperationShape>()

        return operations.flatMap { it.errors }.map { ctx.model.expectShape(it) }.toSet()
    }

    override fun renderConfigure(writer: KotlinWriter) {
        val errors = getModeledErrors()

        errors.forEach { errShape ->
            val code = errShape.id.name
            val symbol = ctx.symbolProvider.toSymbol(errShape)
            val deserializerName = "${symbol.name}Deserializer"
            val httpStatusCode: Int? = errShape.getTrait(HttpErrorTrait::class.java).map { it.code }.orElse(null)
            if (httpStatusCode != null) {
                writer.write("register(code = \$S, deserializer = $deserializerName(), httpStatusCode = $httpStatusCode)", code)
            }
        }
    }
}
