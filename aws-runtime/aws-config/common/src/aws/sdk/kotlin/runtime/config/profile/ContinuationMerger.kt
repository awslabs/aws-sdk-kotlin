/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.profile

/**
 * Merges property continuation lines
 *
 * A property continuation adds additional information to the value of the previous property definition. For example,
 * region = us- followed by the line west-2 defines the value of the region property to be us-\nwest-2. This
 * whitespace-based continuation is described in more formal details in RFC 882, section 3.1.1.
 *
 * @param input ordered lines of an AWS configuration file
 * @return the passed file with continuations merged into single lines
 */
internal fun mergeContinuations(input: String): List<FileLine> =
    buildList {
        var seenPropertyDefinition = false
        var seenProfileDefinition = false

        input
            .split(Literals.NEW_LINE)
            .mapIndexed { index, line -> FileLine(index + 1, line) }
            .filter { it.content.isNotBlank() && !it.content.isCommentLine() }
            .forEach {
                when {
                    it.content.isProfileLine() -> add(it).also { seenProfileDefinition = true; seenPropertyDefinition = false }
                    it.content.isPropertyLine() -> add(it).also { seenProfileDefinition = false; seenPropertyDefinition = true }
                    it.content.isContinuationLine() && seenPropertyDefinition -> mergeContinuation(it, this)
                    it.content.isContinuationLine() && !seenProfileDefinition -> error("Expected a profile definition on: $it")
                    it.content.isContinuationLine() && !seenPropertyDefinition -> error("Expected a property definition on: $it")
                    else -> add(it) // Unknown content. Keep it so that downstream parser will throw the appropriate error
                }
            }
    }

// The line of a file with its position
internal data class FileLine(val lineNumber: Int, val content: String)

/**
 * Merge the [continuation] into the preceding line in [lines].
 */
private fun mergeContinuation(continuation: FileLine, lines: MutableList<FileLine>) {
    val lastLine = lines.removeLast()

    // If a blank (empty valued) property is followed by a property continuation, the property continuation is considered
    // to be a sub-property of the blank property. In this case, the property continuation line must be parsed in the
    // same manner as a Property Definition, except that comments are also considered part of the sub-property's value
    if (lastLine.content.isSubPropertyKeyLine() && !continuation.content.isSubPropertyLine()) {
        throw AwsConfigParseException("Expected '${Literals.PROPERTY_SPLITTER}' specifying a sub-property")
    }

    // A property continuation line is prefixed with \n before it is appended to the property value.
    val updated = "${lastLine.content}${Literals.NEW_LINE}${continuation.content.trim()}"
    lines.add(FileLine(lastLine.lineNumber, updated))
}

// true if this is a comment line
private fun String.isCommentLine() = startsWith(Literals.COMMENT_1) || startsWith(Literals.COMMENT_2)
// true if this is a property line
private fun String.isPropertyLine() = !isProfileLine() && !isContinuationLine() && contains(Literals.PROPERTY_SPLITTER)
// true if this is a profile line.  Use Configuration variant as it can handle both forms.
internal fun String.isProfileLine() = startsWith(Literals.PROFILE_PREFIX)
// true if this is a continuation line
internal fun String.isContinuationLine() = first().isWhitespace() && substring(1).isNotBlank()
// true if this is a sub-property line
private fun String.isSubPropertyLine() = contains(Literals.PROPERTY_SPLITTER) && trim().first() != Literals.PROPERTY_SPLITTER
// true if line is the key part of a property definition of a continuation
private fun String.isSubPropertyKeyLine() = trimEnd().endsWith(Literals.PROPERTY_SPLITTER)

/**
 * Replicates experimental function of the same name in stdlib.  Usage allows for not opting into experimental APIs
 *
 * TODO: When/if the stdlib version becomes stable this should be removed
 */
internal fun <T> buildList(block: MutableList<T>.() -> Unit): List<T> = mutableListOf<T>().apply(block)
