/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.processor

import aws.sdk.kotlin.hll.codegen.core.CodeGeneratorFactory
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbItem
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

private val annotationName = DynamoDbItem::class.qualifiedName!!

public class MapperProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private var invoked = false
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator
    private val codeGeneratorFactory = CodeGeneratorFactory(codeGenerator, logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) { return listOf() }
        invoked = true

        logger.info("Searching for symbols annotated with $annotationName")
        val annotated = resolver.getSymbolsWithAnnotation(annotationName)
        val invalid = annotated.filterNot { it.validate() }.toList()
        logger.info("Found invalid classes $invalid")

        annotated
            .toList()
            .also { logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
//            .associateWith {  }

        return invalid
    }

    private inner class ClassVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val basePackageName = classDeclaration.packageName.asString()
            val packageName = "$basePackageName.mapper.schemas"

            val className = classDeclaration.qualifiedName!!.getShortName()
            val builderName = "${className}Builder"
            val converterName = "${className}Converter"
            val schemaName = "${className}Schema"

            val props = classDeclaration.getAllProperties().mapNotNull(Property.Companion::from)
            val keyProp = checkNotNull(props.singleOrNull { it.isPk }) {
                "Expected exactly one @DynamoDbPartitionKey annotation on a property"
            }

            env.codeGenerator.createNewFile(
            ).use { file ->
                file.bufferedWriter().use { writer ->
                    writer.append(
                        """
                            |package $packageName
                            |
                            |import aws.sdk.kotlin.hll.dynamodbmapper.*
                            |import aws.sdk.kotlin.hll.dynamodbmapper.items.*
                            |import aws.sdk.kotlin.hll.dynamodbmapper.model.*
                            |import aws.sdk.kotlin.hll.dynamodbmapper.values.*
                            |import $basePackageName.$className
                            |
                            |public class $builderName {
                                 ${generateProperties(props)}
                            |    ${generateBuildMethod(className, props)}
                            |}
                            |
                            |public object $converterName : ItemConverter<$className> by SimpleItemConverter(
                            |    builderFactory = ::$builderName,
                            |    build = $builderName::build,
                            |    descriptors = arrayOf(
                                     ${generateDescriptors(className, builderName, props)}
                            |    ),
                            |)
                            |
                            |public object $schemaName : ItemSchema.PartitionKey<$className, ${keyProp.typeName.getShortName()}> {
                            |    override val converter: $converterName = $converterName
                            |    override val partitionKey: KeySpec<${keyProp.keySpecType}> = ${generateKeySpec(keyProp)}
                            |}
                            |
                            |public fun DynamoDbMapper.get${className}Table(name: String): Table.PartitionKey<$className, ${keyProp.typeName.getShortName()}> = getTable(name, $schemaName)
                            |
                        """.trimMargin(),
                    )
                }
            }
        }

        private fun generateBuildMethod(className: String, props: Sequence<Property>) =
            buildString {
                appendLine("public fun build(): $className {")

                props.forEach { prop ->
                    appendLine("""        val ${prop.name} = requireNotNull(${prop.name}) { "Missing value for $className.${prop.name}" }""")
                }

                appendLine()

                append("        return $className(")
                append(props.joinToString(", ") { it.name })
                appendLine(")")

                appendLine("    }")
            }.trimEnd()

        private fun generateDescriptors(
            className: String,
            builderName: String,
            props: Sequence<Property>,
        ) = buildString {
            props.forEach { prop ->
                val converterType = when (val fqTypeName = prop.typeName.asString()) {
                    "aws.smithy.kotlin.runtime.time.Instant" -> "InstantConverter.Default"
                    "kotlin.Boolean" -> "BooleanConverter"
                    "kotlin.Int" -> "IntConverter"
                    "kotlin.String" -> "StringConverter"
                    else -> error("Unsupported attribute type $fqTypeName")
                }

                append("|        AttributeDescriptor(")

                // key
                append("\"")
                append(prop.ddbName)
                append("\", ")

                // getter
                append(className)
                append("::")
                append(prop.name)
                append(", ")

                // setter
                append(builderName)
                append("::")
                append(prop.name)
                append("::set, ")

                // converter
                append(converterType)

                appendLine("),")
            }
        }.trimEnd()

        private fun generateKeySpec(keyProp: Property) = buildString {
            append("KeySpec.")
            append(keyProp.keySpecType)
            append("(\"")
            append(keyProp.name)
            append("\")")
        }

        private fun generateProperties(props: Sequence<Property>) = buildString {
            props.forEach { prop ->
                append("|    public var ")
                append(prop.name)
                append(": ")
                append(prop.typeName.asString())
                appendLine("? = null")
            }
        }
    }
}