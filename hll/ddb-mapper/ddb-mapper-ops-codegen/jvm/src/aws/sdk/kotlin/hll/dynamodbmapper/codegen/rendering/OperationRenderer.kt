/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.*
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Member
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Operation
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Types

// FIXME handle paginated operations differently (e.g., don't map pagination parameters, provide only Flow API)

internal class OperationRenderer(
    private val ctx: RenderContext,
    val operation: Operation,
) : RendererBase(ctx, operation.name) {
    companion object {
        fun factoryFunctionName(operation: Operation) = "${operation.methodName}Operation"
    }

    override fun generate() {
        renderRequest()
        blankLine()
        renderResponse()
        blankLine()
        renderOperationFactory()
    }

    private fun renderOperationFactory() {
        val factoryName = factoryFunctionName(operation)

        withBlock("internal fun <T> #L(table: #T) = #T(", ")", factoryName, Types.tableSpec("T"), Types.Operation) {
            write(
                "initialize = { hReq: #T -> #T(hReq, table.schema, #T(table, #S)) },",
                operation.request.type,
                Types.HReqContextImpl,
                Types.MapperContextImpl,
                operation.name,
            )

            write("serialize = { hReq, schema -> hReq.convert(table.name, schema) },")
            write("lowLevelInvoke = table.mapper.client::#L,", operation.methodName)
            write("deserialize = #L::convert,", operation.response.lowLevelName)
            write("interceptors = table.mapper.config.interceptors,")
        }
    }

    private fun renderRequest() {
        ctx.info("For type ${operation.request.lowLevelName}:")
        val members = operation.request.lowLevel.members.groupBy { m ->
            m.codegenBehavior.also { ctx.info("  ${m.name} → $it") }
        }

        DataTypeGenerator(ctx, this, operation.request).generate()
        blankLine()

        imports += ImportDirective(operation.request.lowLevel.type as TypeRef, operation.request.lowLevelName)

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

        imports += ImportDirective(operation.response.lowLevel.type as TypeRef, operation.response.lowLevelName)

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
