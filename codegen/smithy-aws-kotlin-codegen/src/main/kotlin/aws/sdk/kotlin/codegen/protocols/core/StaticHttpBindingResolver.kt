/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.core

import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingDescriptor
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

/**
 * An HTTP binding resolver that uses a (synthetic) static [HttpTrait]
 *
 * NOTE: The [HttpTrait] is not attached or otherwise associated with the models (otherwise use [HttpTraitResolver]).
 */
open class StaticHttpBindingResolver(
    protected val model: Model,
    protected val service: ServiceShape,
    protected val httpTrait: HttpTrait,
    protected val defaultContentType: String,
    protected val defaultTimestampFormat: TimestampFormatTrait.Format
) : HttpBindingResolver {
    constructor(
        context: ProtocolGenerator.GenerationContext,
        httpTrait: HttpTrait,
        defaultContentType: String,
        defaultTimestampFormat: TimestampFormatTrait.Format
    ) : this(context.model, context.service, httpTrait, defaultContentType, defaultTimestampFormat)

    protected val topDownIndex: TopDownIndex = TopDownIndex.of(model)

    override fun determineRequestContentType(operationShape: OperationShape): String = defaultContentType

    override fun httpTrait(operationShape: OperationShape): HttpTrait = httpTrait

    override fun determineTimestampFormat(
        member: ToShapeId,
        location: HttpBinding.Location,
        defaultFormat: TimestampFormatTrait.Format
    ): TimestampFormatTrait.Format = defaultTimestampFormat

    /**
     * All operations are binding for the model.
     */
    override fun bindingOperations(): List<OperationShape> =
        topDownIndex.getContainedOperations(service).toList()

    /**
     * By default returns all inputs as [HttpBinding.Location.DOCUMENT]
     */
    override fun requestBindings(operationShape: OperationShape): List<HttpBindingDescriptor> {
        if (!operationShape.input.isPresent) return emptyList()
        val input = model.expectShape(operationShape.input.get())
        return input.members().map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT) }.toList()
    }

    /**
     * By default returns all outputs as [HttpBinding.Location.DOCUMENT]
     */
    override fun responseBindings(shape: Shape): List<HttpBindingDescriptor> = when (shape) {
        is OperationShape ->
            shape
                .output
                .map { model.expectShape(it).members() }
                .orElseGet { emptyList() }
                .map { member -> HttpBindingDescriptor(member, HttpBinding.Location.DOCUMENT) }
        is StructureShape -> shape.members().map { member -> member.toHttpBindingDescriptor() }.toList()
        else -> error("unimplemented shape type for http response bindings: $shape")
    }
}

// Create a [HttpBindingDescriptor] based on traits on [MemberShape]
// See https://awslabs.github.io/smithy/1.0/spec/core/http-traits.html
private fun MemberShape.toHttpBindingDescriptor(): HttpBindingDescriptor =
    when {
        hasTrait<HttpHeaderTrait>() -> HttpBindingDescriptor(this, HttpBinding.Location.HEADER, expectTrait<HttpHeaderTrait>().value)
        hasTrait<HttpLabelTrait>() -> HttpBindingDescriptor(this, HttpBinding.Location.LABEL)
        hasTrait<HttpPayloadTrait>() -> HttpBindingDescriptor(this, HttpBinding.Location.PAYLOAD)
        hasTrait<HttpQueryTrait>() -> HttpBindingDescriptor(this, HttpBinding.Location.QUERY, expectTrait<HttpQueryTrait>().value)
        hasTrait<HttpResponseCodeTrait>() -> HttpBindingDescriptor(this, HttpBinding.Location.RESPONSE_CODE)
        hasTrait<HttpPrefixHeadersTrait>() -> HttpBindingDescriptor(this, HttpBinding.Location.PREFIX_HEADERS, expectTrait<HttpPrefixHeadersTrait>().value)
        // By default, all structure members that are not bound as part of the HTTP message are
        // serialized in a protocol-specific document sent in the body of the message
        else -> HttpBindingDescriptor(this, HttpBinding.Location.DOCUMENT)
        // NOTE: Unsure of where (if anywhere) HttpBinding.Location.UNBOUND should be modeled
    }
