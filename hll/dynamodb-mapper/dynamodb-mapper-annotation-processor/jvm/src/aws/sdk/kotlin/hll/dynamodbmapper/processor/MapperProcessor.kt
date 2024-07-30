/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.processor

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

public class MapperProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        env.logger.info("Searching for symbols annotated with $annotationName")
        val annotated = resolver.getSymbolsWithAnnotation(annotationName)
        val invalid = annotated.filterNot { it.validate() }.toList()
        env.logger.info("Found invalid classes $invalid")

        annotated
            .toList()
            .also { env.logger.info("Found annotated classes: $it") }
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.validate() }
            .forEach { it.accept(ItemVisitor(), Unit) }

        return invalid
    }

    private inner class ItemVisitor : KSVisitorVoid() {
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
                Dependencies(true, classDeclaration.containingFile!!),
                packageName,
                schemaName,
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

private data class Property(val name: String, val ddbName: String, val typeName: KSName, val isPk: Boolean) {
    companion object {
        @OptIn(KspExperimental::class)
        fun from(ksProperty: KSPropertyDeclaration) = ksProperty
            .getter
            ?.returnType
            ?.resolve()
            ?.declaration
            ?.qualifiedName
            ?.let { typeName ->
                val isPk = ksProperty.isAnnotationPresent(DynamoDbPartitionKey::class)
                val name = ksProperty.simpleName.getShortName()
                val ddbName = ksProperty.getAnnotationsByType(DynamoDbAttribute::class).singleOrNull()?.name ?: name
                Property(name, ddbName, typeName, isPk)
            }
    }
}

private val Property.keySpecType: String
    get() = when (val fqTypeName = typeName.asString()) {
        "kotlin.Int" -> "Number"
        "kotlin.String" -> "String"
        else -> error("Unsupported key type $fqTypeName, expected Int or String")
    }
