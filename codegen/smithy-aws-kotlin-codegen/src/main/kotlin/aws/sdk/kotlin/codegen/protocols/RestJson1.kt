/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.json.AwsJsonProtocolParserGenerator
import aws.sdk.kotlin.codegen.protocols.json.JsonHttpBindingProtocolGenerator
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.StructuredDataParserGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.HttpBinding
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class RestJson1 : JsonHttpBindingProtocolGenerator() {

    override val protocol: ShapeId = RestJson1Trait.ID

    override fun getProtocolHttpBindingResolver(model: Model, serviceShape: ServiceShape): HttpBindingResolver =
        HttpTraitResolver(model, serviceShape, ProtocolContentTypes.consistent("application/json"))

    override fun renderSerializeHttpBody(
        ctx: ProtocolGenerator.GenerationContext,
        op: OperationShape,
        writer: KotlinWriter,
    ) {
        super.renderSerializeHttpBody(ctx, op, writer)

        val resolver = getProtocolHttpBindingResolver(ctx.model, ctx.service)
        if (!resolver.hasHttpBody(op)) return

        // restjson1 has some different semantics and expectations around empty structures bound via @httpPayload trait
        //   * empty structures get serialized to `{}`
        // see: https://github.com/awslabs/smithy/pull/924
        val requestBindings = resolver.requestBindings(op)
        val httpPayload = requestBindings.firstOrNull { it.location == HttpBinding.Location.PAYLOAD }
        if (httpPayload != null) {
            // explicit payload member as the sole payload
            val memberName = httpPayload.member.defaultName()
            val target = ctx.model.expectShape(httpPayload.member.target)
            writer.withBlock("if (input.#L == null) {", "}", memberName) {
                if (target is StructureShape) {
                    addImport(RuntimeTypes.Http.ByteArrayContent)
                    write("builder.body = #T(#S.encodeToByteArray())", RuntimeTypes.Http.ByteArrayContent, "{}")
                }
                // Content-Type still needs to be set for non-structured payloads
                // https://github.com/awslabs/smithy/blob/main/smithy-aws-protocol-tests/model/restJson1/http-content-type.smithy#L174
                write("builder.headers.setMissing(\"Content-Type\", #S)", resolver.determineRequestContentType(op))
            }
        }
    }

    override fun structuredDataParser(ctx: ProtocolGenerator.GenerationContext): StructuredDataParserGenerator =
        AwsJsonProtocolParserGenerator(this, supportsJsonNameTrait)
}
