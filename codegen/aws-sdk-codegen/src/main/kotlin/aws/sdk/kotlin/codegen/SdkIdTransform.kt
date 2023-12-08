/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

private val whitespaceRegex = Regex("\\s")

/**
 * Implements a single standardized sdkId transform.
 */
interface SdkIdTransformer {
    fun transform(id: String): String
}

/**
 * Implements all standardized sdkId transforms.
 */
object SdkIdTransform {
    /**
     * Replace all whitespace from the sdkId with underscores and lowercase all letters.
     */
    object LowerSnakeCase : SdkIdTransformer {
        override fun transform(id: String): String = id.replaceWhitespace("_").lowercase()
    }

    /**
     * None. Directly use the sdkId.
     */
    object Identity : SdkIdTransformer {
        override fun transform(id: String): String = id
    }

    /**
     * Remove all whitespace from the sdkId.
     */
    object NoWhitespace : SdkIdTransformer {
        override fun transform(id: String): String = id.replaceWhitespace("")
    }

    /**
     * Replace all whitespace from the sdkId with dashes and lowercase all letters.
     */
    object LowerKebabCase : SdkIdTransformer {
        override fun transform(id: String): String = id.replaceWhitespace("-").lowercase()
    }

    /**
     * Replace all whitespace from the sdkId with underscores and capitalize all letters.
     */
    object UpperSnakeCase : SdkIdTransformer {
        override fun transform(id: String): String = id.replaceWhitespace("_").uppercase()
    }
}

/**
 * Applies a concrete sdkId transform to a string.
 */
fun String.withTransform(transformer: SdkIdTransformer): String = transformer.transform(this)

private fun String.replaceWhitespace(replacement: String) = replace(whitespaceRegex, replacement)
