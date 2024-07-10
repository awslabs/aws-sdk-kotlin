/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.core

internal class TemplateEngine(processors: List<TemplateProcessor>) {
    private val processors = processors.associate { it.key to it.handler }
    private val parameterRegex = """(?<!#)#([1-9][0-9]*)?([A-Z])""".toRegex()

    fun process(template: String, args: List<Any>): String {
        var allIndexed: Boolean? = null

        return parameterRegex.replaceIndexed(template) { pos, result ->
            val explicitIndex = result.groupValues[1].takeUnless { it.isEmpty() }?.toInt()?.minus(1)
            val indexed = explicitIndex != null

            fun req(condition: Boolean, message: () -> String) {
                require(condition) {
                    buildString {
                        append("Error in template '")
                        append(template)
                        append("' for parameter '")
                        append(result.value)
                        append("' at index ")
                        append(result.range.first)
                        append(": ")

                        append(message())
                    }
                }
            }

            fun <T> reqNotNull(element: T?, message: () -> String): T {
                req(element != null, message)
                return element!!
            }

            req(allIndexed == null || allIndexed == indexed) {
                "Cannot mix indexed and non-indexed parameters in the same template"
            }
            allIndexed = indexed

            val key = result.groupValues[2].toCharArray().single()
            val handler = reqNotNull(processors[key]) { "No processor found for key character '$key'" }

            val index = explicitIndex ?: pos
            req(index in args.indices) {
                buildString {
                    if (indexed) {
                        append("Index ")
                        append(result.groupValues[1])
                    } else {
                        append("Parameter index ")
                        append(pos)
                    }
                    append(" is outside of args bounds ")
                    append(args.indices)
                }
            }

            handler(args[index])
        }
    }
}

private fun Regex.replaceIndexed(input: CharSequence, transform: (index: Int, MatchResult) -> CharSequence): String {
    var index = 0
    return replace(input) {
        transform(index, it).also { index++ }
    }
}
