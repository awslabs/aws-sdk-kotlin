/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
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
abstract class ModeledExceptionsFeature(
    protected val ctx: ProtocolGenerator.GenerationContext,
    protected val httpBindingResolver: HttpBindingResolver
    ) : HttpFeature {

    protected fun getModeledErrors(): Set<Shape> {
        val operations = httpBindingResolver.resolveBindingOperations()

        return operations.flatMap { it.errors }.map { ctx.model.expectShape(it) }.toSet()
    }

    abstract override fun renderConfigure(writer: KotlinWriter)
}
