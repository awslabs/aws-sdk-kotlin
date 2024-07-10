/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.core

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.Type
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.TypeRef
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.quote

internal data class TemplateProcessor(val key: Char, val handler: (Any) -> String) {
    companion object {
        inline fun <reified T> typed(key: Char, crossinline handler: (T) -> String) = TemplateProcessor(key) { value ->
            require(value is T) { "Expected argument of type ${T::class} but found $value" }
            handler(value)
        }

        val Literal = TemplateProcessor('L') { it.toString() }

        val QuotedString = typed<String>('S') { it.quote() }

        fun forType(pkg: String, imports: ImportDirectives): TemplateProcessor {
            val processor = ImportingTypeProcessor(pkg, imports)
            return typed<Type>('T', processor::format)
        }
    }

    init {
        require(key in 'A'..'Z') { "Key character must be a capital letter (A-Z)" }
    }
}

private open class TypeProcessor {
    open fun format(type: Type): String = buildString {
        append(type.shortName)
        if (type is TypeRef && type.genericArgs.isNotEmpty()) {
            type.genericArgs.joinToString(", ", "<", ">", transform = ::format).let(::append)
        }

        if (type.nullable) append('?')
    }
}

private class ImportingTypeProcessor(private val pkg: String, private val imports: ImportDirectives) : TypeProcessor() {
    override fun format(type: Type): String = buildString {
        if (type is TypeRef && type.pkg != pkg) {
            val existingImport = imports[type.shortName]

            if (existingImport == null) {
                imports += ImportDirective(type)
            } else if (existingImport.fullName != type.fullName) {
                append(type.pkg)
                append('.')
            }
        }

        append(super.format(type))
    }
}
