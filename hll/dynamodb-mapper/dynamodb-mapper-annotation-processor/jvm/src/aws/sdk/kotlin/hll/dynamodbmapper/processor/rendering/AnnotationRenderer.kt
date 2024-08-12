/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.processor.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

public class AnnotationRenderer(
    private val classDeclaration: KSClassDeclaration,
    private val ctx: RenderContext
) : RendererBase(ctx, "${classDeclaration.qualifiedName!!.getShortName()}Schema") {
    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val builderName = "${className}Builder"
    private val converterName = "${className}Converter"
    private val schemaName = "${className}Schema"

    private val properties = classDeclaration.getAllProperties().mapNotNull(Property.Companion::from)
    private val keyProperty = checkNotNull(properties.singleOrNull { it.isPk }) {
        "Expected exactly one @DynamoDbPartitionKey annotation on a property"
    }

    override fun generate() {
        imports.add(ImportDirective(classDeclaration.qualifiedName!!.asString())) // Import the class that's getting processed
        renderBuilder()
        renderItemConverter()
        renderSchema()
        renderGetTable()
    }

    private fun renderBuilder() {
        checkNotNull(properties.singleOrNull { it.isPk }) { "Expected exactly one @DynamoDbPartitionKey annotation on a property" }

        withDocs {
            write("A DSL-style builder for instances of [#L]", className)
        }

        withBlock("public class #L {", "}", builderName) {
            properties.forEach {
                write("public var #L: #L? = null", it.name, it.typeName.asString().removePrefix("kotlin."))
            }
            blankLine()

            withBlock("public fun build(): #L {", "}", className) {
                properties.forEach {
                    write("val #1L = requireNotNull(#1L) { #2S }", it.name, "Missing value for ${it.name}")
                }
                blankLine()
                withBlock("return #L(", ")", className) {
                    properties.forEach {
                        write("#L,", it.name)
                    }
                }
            }
        }
        blankLine()
    }

    private fun renderItemConverter() {
        withBlock("internal object #L : #T<#L> by #T(", ")", converterName, Types.ItemConverter, className, Types.SimpleItemConverter) {
            write("builderFactory = ::#L,", builderName)
            write("build = #L::build,", builderName)
            withBlock("descriptors = arrayOf(", "),") {
                properties.forEach {
                    renderAttributeDescriptor(it)
                }
            }
        }
        blankLine()
    }

    private fun renderAttributeDescriptor(prop: Property) {
        withBlock("#T(", "),", Types.AttributeDescriptor) {
            write("#S,", prop.ddbName) // key
            write("#L,", "${className}::${prop.name}") // getter
            write("#L,", "${builderName}::${prop.name}::set") // setter
            write("#T", prop.valueConverter) // converter
        }
    }

    private val Property.valueConverter: Type
        get() = when (typeName.asString()) {
            "aws.smithy.kotlin.runtime.time.Instant" -> Types.DefaultInstantConverter
            "kotlin.Boolean" -> Types.BooleanConverter
            "kotlin.Int" -> Types.IntConverter
            "kotlin.String" -> Types.StringConverter
            else -> error("Unsupported attribute type ${typeName.asString()}")
        }

    private fun renderSchema() {
        withBlock("internal object #L : #T.#L<#L, #L> {", "}", schemaName, Types.ItemSchema, "PartitionKey", className, keyProperty.typeName.getShortName()) {
            write("override val converter : #1L = #1L", converterName)
            write("override val partitionKey: #1T<#2L> = #1T.#2L(#3S)", Types.KeySpec, keyProperty.keySpec, keyProperty.name)
        }
        blankLine()
    }

    private val Property.keySpec: String
        get() = when (typeName.asString()) {
            "kotlin.Int" -> "Number"
            "kotlin.String" -> "String"
            else -> error("Unsupported key type ${typeName.asString()}, expected Int or String")
        }

    private fun renderGetTable() {
        withDocs {
            write("Returns a reference to a table named [name] containing items representing [#L]", className)
        }
        write("public fun #1T.get#2LTable(name: String): #3T.#4L<#2L, #5L> = #6L(name, #7L)",
            Types.DynamoDbMapper,
            className,
            Types.Table,
            "PartitionKey",
            keyProperty.typeName.getShortName(),
            "getTable",
            schemaName
        )
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
