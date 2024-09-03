/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.DestinationPackageAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateBuilderClassesAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateGetTableMethodAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.VisibilityAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering.HighLevelRenderer
import aws.smithy.kotlin.runtime.collections.*
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

private val annotationName = DynamoDbItem::class.qualifiedName!!

class AnnotationsProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var invoked = false
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val codeGeneratorFactory = CodeGeneratorFactory(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return listOf()
        }
        invoked = true

        logger.info("Searching for symbols annotated with $annotationName")
        val annotated = resolver.getSymbolsWithAnnotation(annotationName)
        val invalid = annotated.filterNot { it.validate() }.toList()
        logger.info("Found invalid classes $invalid")

        val annotatedClasses = annotated
            .toList()
            .also { logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }

        HighLevelRenderer(annotatedClasses, logger, codeGeneratorFactory, getCodegenAttributes()).render()

        return invalid
    }

    /**
     * Parse and validate the KSP environment options, turning them into valid attribute values
     */
    private fun getCodegenAttributes(): Attributes {
        val generateGetTableMethod = environment.options.getOrDefault(GenerateGetTableMethodAttribute.name, "true")
        check(generateGetTableMethod.equals("true", ignoreCase = true) || generateGetTableMethod.equals("false", ignoreCase = true)) { "Unsupported value for ${GenerateGetTableMethodAttribute.name}, expected \"true\" or \"false\", got $generateGetTableMethod" }

        return attributesOf {
            GenerateBuilderClassesAttribute to GenerateBuilderClasses.valueOf(environment.options[GenerateBuilderClassesAttribute.name] ?: GenerateBuilderClasses.WHEN_REQUIRED.name)
            VisibilityAttribute to Visibility.valueOf(environment.options.getOrDefault(VisibilityAttribute.name, Visibility.DEFAULT.name))
            DestinationPackageAttribute to DestinationPackage.fromString(environment.options.getOrDefault(DestinationPackageAttribute.name, "relative=aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas"))
            GenerateGetTableMethodAttribute to generateGetTableMethod.equals("true", ignoreCase = true)
        }
    }
}
