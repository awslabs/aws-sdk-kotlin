/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.core

import aws.sdk.kotlin.hll.codegen.core.CodeGenerator
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.*
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Member
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Structure
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.rendering.RenderContext

/**
 * Generates immutable data types from a [Structure] into an underlying [CodeGenerator]. These data types consist of a
 * read-only `interface` (with a `val` field for each [Structure] member), a `private data class` that serves as the
 * default implementation, and a `public` factory function which allows constructing new instances by passing values as
 * arguments.
 *
 * **FIXME**: This generator SHOULD generate a mutable builder object instead of a factory function for backwards
 * compatibility.
 *
 * Example of generated data type:
 *
 * ```kotlin
 * public interface User {
 *     public companion object { }
 *
 *     public val id: Int
 *     public val name: String
 *     public val active: Boolean
 * }
 *
 * private data class UserImpl(
 *     override val id: Int,
 *     override val name: String,
 *     override val active: Boolean,
 * )
 *
 * public fun User(
 *     id: Int,
 *     name: String,
 *     active: Boolean,
 * ): User = UserImpl(
 *     id,
 *     name,
 *     active,
 * )
 * ```
 * @param ctx The active rendering context
 * @param generator The underlying generator for the context into which the data type should be written
 * @param structure The [Structure] which describes the data type for which to generate code
 */
class DataTypeGenerator(
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

        val genericParams = structure
            .members
            .flatMap { it.type.generics() }
            .map { it.shortName }
            .requireAllDistinct()
            .takeUnless { it.isEmpty() }
            ?.joinToString(", ", "<", ">")
            ?: ""

        val implName = "${structure.type.shortName}Impl"
        openBlock("private data class #L#L(", implName, genericParams)
        members { write("override val #L: #T,", name, type) }
        closeBlock("): #T", structure.type)
        blankLine()

        // TODO replace function builder with Builder interface+impl
        openBlock("public fun #L #L(", genericParams, structure.type.shortName)
        members { write("#L: #T,", name, type) }
        closeAndOpenBlock("): #T = #L(", structure.type, implName)
        members { write("#L,", name) }
        closeBlock(")")
    }

    private inline fun members(crossinline block: Member.() -> Unit) {
        structure.members.forEach { it.block() }
    }
}

private fun Type.generics(): List<TypeVar> = buildList {
    when (val type = this@generics) {
        is TypeVar -> add(type)
        is TypeRef -> type.genericArgs.flatMap { it.generics() }
    }
}

private fun <T, C : Iterable<T>> C.requireAllDistinct(): C {
    val collection = this
    val itemCounts = buildMap {
        collection.forEach { element ->
            compute(element) { _, existingCount -> (existingCount ?: 0) + 1 }
        }
    }

    val duplicates = itemCounts.filter { (_, count) -> count > 1 }.keys
    require(duplicates.isEmpty()) { "Found duplicated items: $duplicates" }

    return collection
}
