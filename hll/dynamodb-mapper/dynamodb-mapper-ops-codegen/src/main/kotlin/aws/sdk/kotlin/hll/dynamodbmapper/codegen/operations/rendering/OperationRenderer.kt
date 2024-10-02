/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.*
import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.model.Structure
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.rendering.info
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model.*

/**
 * Renders a dedicated file for a high-level operation, including request/response types, converters between low/high
 * types, and a factory method for creating instances of the DDB mapper runtime operation
 * @param ctx The active [RenderContext]
 * @param operation The [Operation] to codegen
 */
internal class OperationRenderer(
    private val ctx: RenderContext,
    private val operation: Operation,
) : RendererBase(ctx, operation.name) {
    private val members = operation.request.lowLevel.members.groupBy { m ->
        m.codegenBehavior.also { ctx.info("  ${m.name} → $it") }
    }

    companion object {
        fun factoryFunctionName(operation: Operation) = "${operation.methodName}Operation"
    }

    override fun generate() {
        renderRequest()
        blankLine()
        renderResponse()

        renderOperationFactory()
    }

    private fun renderOperationFactory() {
        val factoryName = factoryFunctionName(operation)

        operation.itemSourceKinds.filterNot { it.isAbstract }.forEach { itemSourceKind ->
            blankLine()
            withBlock(
                "internal fun <T> #L(spec: #T) = #T(",
                ")",
                factoryName,
                itemSourceKind.getSpecType("T"),
                MapperTypes.PipelineImpl.Operation,
            ) {
                write(
                    "initialize = { highLevelReq: #T -> #T(highLevelReq, spec.schema, #T(spec, #S)) },",
                    operation.request.type,
                    MapperTypes.PipelineImpl.HReqContextImpl,
                    MapperTypes.PipelineImpl.MapperContextImpl,
                    operation.name,
                )

                writeInline("serialize = { highLevelReq, schema -> highLevelReq.convert(")
                members(MemberCodegenBehavior.Hoist) {
                    if (name in itemSourceKind.hoistedFields) {
                        writeInline("spec.#L, ", name)
                    } else {
                        writeInline("#L = null, ", name)
                    }
                }
                write("schema) },")

                withBlock("lowLevelInvoke = { lowLevelReq ->", "},") {
                    withBlock("spec.mapper.client.#T { client ->", "}", MapperTypes.Internal.withWrappedClient) {
                        write("client.#L(lowLevelReq)", operation.methodName)
                    }
                }

                write("deserialize = #L::convert,", operation.response.lowLevelName)
                write("interceptors = spec.mapper.config.interceptors,")
            }
        }
    }

    private fun renderRequest() {
        DataTypeGenerator(ctx, this, operation.request).generate()
        blankLine()

        imports += ImportDirective(operation.request.lowLevel.type, operation.request.lowLevelName)

        openBlock("private fun <T> #T.convert(", operation.request.type)
        members(MemberCodegenBehavior.Hoist) { write("#L: #T, ", name, type) }
        write("schema: #T,", MapperTypes.Items.itemSchema("T"))
        closeAndOpenBlock(") = #L {", operation.request.lowLevelName)
        members(MemberCodegenBehavior.PassThrough) { write("#1L = this@convert.#1L", name) }
        members(MemberCodegenBehavior.MapKeys) {
            write("this@convert.#1L?.let { #1L = schema.converter.convertTo(it, schema.keyAttributeNames) }", name)
        }
        members(MemberCodegenBehavior.MapAll) {
            write("this@convert.#1L?.let { #1L = schema.converter.convertTo(it) }", name)
        }
        members(MemberCodegenBehavior.ListMapAll) {
            write("#1L = this@convert.#1L?.map { schema.converter.toItem(it) }", name)
        }
        members(MemberCodegenBehavior.Hoist) { write("this.#1L = #1L", name) }
        closeBlock("}")
    }

    private fun renderResponse() {
        ctx.info("For type ${operation.response.lowLevelName}:")
        val members = operation.response.lowLevel.members.groupBy { m ->
            m.codegenBehavior.also { ctx.info("  ${m.name} → $it") }
        }

        DataTypeGenerator(ctx, this, operation.response).generate()
        blankLine()

        imports += ImportDirective(operation.response.lowLevel.type, operation.response.lowLevelName)

        withBlock(
            "private fun <T> #L.convert(schema: #T) = #T {",
            "}",
            operation.response.lowLevelName,
            MapperTypes.Items.itemSchema("T"),
            operation.response.type,
        ) {
            members(MemberCodegenBehavior.PassThrough) { write("#1L = this@convert.#1L", name) }

            members(MemberCodegenBehavior.MapKeys, MemberCodegenBehavior.MapAll) {
                write(
                    "#1L = this@convert.#1L?.#2T()?.let(schema.converter::convertFrom)",
                    name,
                    MapperTypes.Model.toItem,
                )
            }

            members(MemberCodegenBehavior.ListMapAll) {
                write(
                    "#1L = this@convert.#1L?.map { schema.converter.convertFrom(it.#2T()) }",
                    name,
                    MapperTypes.Model.toItem,
                )
            }
        }
    }

    private inline operator fun Map<MemberCodegenBehavior, List<Member>>.invoke(
        vararg behaviors: MemberCodegenBehavior,
        block: Member.() -> Unit,
    ) {
        behaviors.forEach { behavior ->
            get(behavior)?.forEach(block)
        }
    }
}

private val Structure.lowLevelName: String
    get() = "LowLevel${type.shortName}"
