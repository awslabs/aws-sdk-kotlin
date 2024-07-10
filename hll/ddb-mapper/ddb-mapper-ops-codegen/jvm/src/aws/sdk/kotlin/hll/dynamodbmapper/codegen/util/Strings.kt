/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.util

private val codepointMap = buildMap {
    (0..255).filter(Character::isISOControl).forEach { code -> put(code, unicodeEscape(code)) }
    put(8, "\\b")
    put(9, "\\t")
    put(10, "\\n")
    put(13, "\\r")
    put(34, "\\\"")
    put(36, "\\\$")
    put(92, "\\\\")
}.withDefault(Character::toString)

private fun unicodeEscape(code: Int): String {
    require(code < 65536) { "This function only works for codepoints < 65536. If you've run into this error, fix me!" }
    return "\\u" + String.format("%04X", code)
}

internal fun String.escape(): String = buildString {
    this@escape.codePoints().forEach { append(codepointMap.getValue(it)) }
}

internal val String.capitalizeFirstChar: String
    get() = replaceFirstChar { it.uppercaseChar() }

internal val String.lowercaseFirstChar: String
    get() = replaceFirstChar { it.lowercaseChar() }

internal fun String.quote(): String = "\"${escape()}\""
