/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering.HighLevelRenderer
import aws.smithy.kotlin.runtime.collections.attributesOf
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateBuilderClassesAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.VisibilityAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.DestinationPackageAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions.GenerateGetTableMethodAttribute

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

        val codegenAttributes = attributesOf {
            GenerateBuilderClassesAttribute to environment.options.getOrDefault(GenerateBuilderClassesAttribute.name, GenerateBuilderClasses.WHEN_REQUIRED)
            VisibilityAttribute to environment.options.getOrDefault(VisibilityAttribute.name, Visibility.IMPLICIT)
            DestinationPackageAttribute.name to environment.options.getOrDefault(DestinationPackageAttribute.name, DestinationPackage.RELATIVE())
            GenerateGetTableMethodAttribute.name to (environment.options.getOrDefault(GenerateGetTableMethodAttribute.name, "true") == "true")
        }

        val annotatedClasses = annotated
            .toList()
            .also { logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }

        HighLevelRenderer(annotatedClasses, logger, codeGeneratorFactory, codegenAttributes).render()

        return invalid
    }
}
