/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.core

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.CodeGenerator as KSCodeGenerator

internal class CodeGeneratorFactory(private val ksCodeGenerator: KSCodeGenerator, private val logger: KSPLogger) {
    private val dependencies = Dependencies.ALL_FILES

    fun generator(name: String, pkg: String): CodeGenerator {
        val imports = ImportDirectives()
        val processors = listOf(
            TemplateProcessor.Literal,
            TemplateProcessor.QuotedString,
            TemplateProcessor.forType(pkg, imports),
        )
        val engine = TemplateEngine(processors)

        val persistCallback: (String) -> Unit = { content ->
            logger.info("Checking out code generator for class $pkg.$name")

            ksCodeGenerator
                .createNewFile(dependencies, pkg, name) // FIXME don't depend on ALL_FILES
                .use { outputStream ->
                    outputStream.writer().use { writer -> writer.append(content) }
                }
        }

        return CodeGeneratorImpl(pkg, engine, persistCallback, imports)
    }
}
