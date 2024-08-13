/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.rendering

import aws.sdk.kotlin.hll.codegen.core.*
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.rendering.info
import aws.sdk.kotlin.hll.dynamodbmapper.operations.rendering.DataTypeGenerator
import aws.sdk.kotlin.hll.dynamodbmapper.operations.model.*

// FIXME handle paginated operations differently (e.g., don't map pagination parameters, provide only Flow API)

/**
 * Renders a dedicated file for a high-level operation, including request/response types, converters between low/high
 * types, and a factory method for creating instances of the DDB mapper runtime operation
 * @param ctx The active [RenderContext]
 * @param operation The [Operation] to codegen
 */
class OperationRenderer(
    private val ctx: RenderContext,
    val operation: Operation,
) : RendererBase(ctx, operation.name, "dynamodb-mapper-ops-codegen") {
    val members = operation.request.lowLevel.members.groupBy { m ->
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
                Types.Operation,
            ) {
                write(
                    "initialize = { highLevelReq: #T -> #T(highLevelReq, spec.schema, #T(spec, #S)) },",
                    operation.request.type,
                    Types.HReqContextImpl,
                    Types.MapperContextImpl,
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

                write("lowLevelInvoke = spec.mapper.client::#L,", operation.methodName)
                write("deserialize = #L::convert,", operation.response.lowLevelName)
                write("interceptors = spec.mapper.config.interceptors,")
            }
        }
    }

    private fun renderRequest() {
        ctx.info("For type ${operation.request.lowLevelName}:")
        DataTypeGenerator(ctx, this, operation.request).generate()
        blankLine()

        imports += ImportDirective(operation.request.lowLevel.type, operation.request.lowLevelName)

        openBlock("private fun <T> #T.convert(", operation.request.type)
        members(MemberCodegenBehavior.Hoist) { write("#L: #T, ", name, type) }
        write("schema: #T,", Types.itemSchema("T"))
        closeAndOpenBlock(") = #L {", operation.request.lowLevelName)
        members(MemberCodegenBehavior.PassThrough) { write("#1L = this@convert.#1L", name) }
        members(MemberCodegenBehavior.MapKeys) {
            write("this@convert.#1L?.let { #1L = schema.converter.toItem(it, schema.keyAttributeNames) }", name)
        }
        members(MemberCodegenBehavior.MapAll) {
            write("this@convert.#1L?.let { #1L = schema.converter.toItem(it) }", name)
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
            "private fun <T> #L.convert(schema: #T) = #T(",
            ")",
            operation.response.lowLevelName,
            Types.itemSchema("T"),
            operation.response.type,
        ) {
            members(MemberCodegenBehavior.PassThrough) { write("#1L = this@convert.#1L,", name) }

            members(MemberCodegenBehavior.MapKeys, MemberCodegenBehavior.MapAll) {
                write("#1L = this@convert.#1L?.#2T()?.let(schema.converter::fromItem),", name, Types.toItem)
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
