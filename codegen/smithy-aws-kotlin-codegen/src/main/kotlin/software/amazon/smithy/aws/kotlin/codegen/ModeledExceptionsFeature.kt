/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.smithy.aws.kotlin.codegen

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
