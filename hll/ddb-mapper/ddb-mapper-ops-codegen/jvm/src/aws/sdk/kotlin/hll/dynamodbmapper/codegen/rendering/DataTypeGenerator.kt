/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Member
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Structure

internal class DataTypeGenerator(
    private val ctx: RenderContext,
    generator: CodeGenerator,
    val structure: Structure,
) : CodeGenerator by generator {
    fun generate() {
        withBlock("public interface #T {", "}", structure.type) {
            write("public companion object { }") // leave room for future expansion
            blankLine()
            members { write("public val #L: #T", name, type) }
        }
        blankLine()

        val implName = "${structure.type.shortName}Impl"
        openBlock("private data class #L<T>(", implName)
        members { write("override val #L: #T,", name, type) }
        closeBlock("): #T", structure.type)
        blankLine()

        // TODO replace function builder with Builder interface+impl
        openBlock("public fun <T> #L(", structure.type.shortName)
        members { write("#L: #T,", name, type) }
        closeAndOpenBlock("): #T = #L(", structure.type, implName)
        members { write("#L,", name) }
        closeBlock(")")
    }

    private inline fun members(crossinline block: Member.() -> Unit) {
        structure.members.forEach { it.block() }
    }
}
