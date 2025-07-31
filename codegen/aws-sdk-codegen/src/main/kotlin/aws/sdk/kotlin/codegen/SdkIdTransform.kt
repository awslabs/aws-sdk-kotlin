/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.utils.toPascalCase

private val whitespaceRegex = Regex("\\s")

private val spaceOrDashRegex = Regex("\\s|-")

/**
 * Base interface for string transformers.
 */
interface StringTransformer {
    fun transform(input: String): String
}

/**
 * Implements a single standardized sdkId transform.
 */
interface SdkIdTransformer : StringTransformer

/**
 * Implements a single standardized SigV4 service signing name transform.
 */
interface SigV4NameTransformer : StringTransformer

/**
 * Implements all standardized sdkId transforms.
 */
object SdkIdTransform {
    /**
     * Replace all whitespace from the sdkId with underscores and lowercase all letters.
     */
    object LowerSnakeCase : SdkIdTransformer {
        override fun transform(input: String): String = input.replaceWhitespace("_").lowercase()
    }

    /**
     * None. Directly use the sdkId.
     */
    object Identity : SdkIdTransformer {
        override fun transform(input: String): String = input
    }

    /**
     * Remove all whitespace from the sdkId.
     */
    object NoWhitespace : SdkIdTransformer {
        override fun transform(input: String): String = input.replaceWhitespace("")
    }

    /**
     * Replace all whitespace from the sdkId with dashes and lowercase all letters.
     */
    object LowerKebabCase : SdkIdTransformer {
        override fun transform(input: String): String = input.replaceWhitespace("-").lowercase()
    }

    /**
     * Replace all whitespace from the sdkId with underscores and capitalize all letters.
     */
    object UpperSnakeCase : SdkIdTransformer {
        override fun transform(input: String): String = input.replaceWhitespace("_").uppercase()
    }
}

object SigV4NameTransform {
    /**
     * Replace all dashes from the SigV4 service signing name with underscores and capitalize all letters.
     */
    object UpperSnakeCase : SigV4NameTransformer {
        override fun transform(input: String): String = input.lowercase().replaceSpaceOrDash("_").uppercase()
    }

    /**
     * Remove dashes and convert SigV4 service signing name to PascalCase
     */
    object PascalCase : SigV4NameTransformer {
        override fun transform(input: String): String = input.toPascalCase()
    }
}

/**
 * Applies the given transformer to the string.
 */
fun <T : StringTransformer> String.withTransform(transformer: T): String = transformer.transform(this)

private fun String.replaceWhitespace(replacement: String) = replace(whitespaceRegex, replacement)

private fun String.replaceSpaceOrDash(replacement: String) = replace(spaceOrDashRegex, replacement)
