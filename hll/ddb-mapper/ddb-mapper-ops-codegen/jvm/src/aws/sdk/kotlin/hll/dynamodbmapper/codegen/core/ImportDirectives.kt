/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.core

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.TypeRef

internal class ImportDirectives : MutableSet<ImportDirective> by mutableSetOf() {
    operator fun get(shortName: String) = firstOrNull { it.shortName == shortName }

    val formatted: String
        get() = buildString {
            sortedWith(importComparator).forEach { appendLine(it.formatted) }
        }
}

private val specialPrefixes = setOf(
    "java.",
    "javax.",
    "kotlin.",
)

private val importComparator = compareBy<ImportDirective> { it.alias != null } // aliased imports at the very end
    .thenBy { directive -> specialPrefixes.any { directive.fullName.startsWith(it) } } // special prefixes < aliases
    .thenBy { it.fullName } // sort alphabetically

internal data class ImportDirective(val fullName: String, val alias: String? = null) {
    val shortName = fullName.split(".").last()
    private val aliasFormatted = alias?.let { " as $it" } ?: ""
    val formatted = "import $fullName$aliasFormatted"
}

internal fun ImportDirective(type: TypeRef, alias: String? = null) = ImportDirective(type.fullName, alias)
