/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.operations.codegen

import java.io.File

class CodegenOrchestrator(val dest: File) {
    fun execute() {
        val fooSrc = buildString {
            appendLine("package foo")
            appendLine()
            appendLine("public class Foo {")
            appendLine("    public fun fooIt(): Unit = println(\"Foo!\")")
            appendLine("}")
        }
        dest.resolve("Foo.kt").writeText(fooSrc)
    }
}
