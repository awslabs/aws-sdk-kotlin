/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.processor.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.codegen.util.Pkg
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
        val basePackageName = classDeclaration.packageName.asString()

        write("package #L", "$basePackageName.mapper.schemas")
        blankLine()
        // TODO replace with import directives
        write("import aws.sdk.kotlin.hll.dynamodbmapper.*")
        write("import aws.sdk.kotlin.hll.dynamodbmapper.items.*")
        write("import aws.sdk.kotlin.hll.dynamodbmapper.model.*")
        write("import aws.sdk.kotlin.hll.dynamodbmapper.values.*")
        write("import $basePackageName.$className")
        blankLine()

        renderBuilder()
        renderItemConverter()
        renderSchema()
        renderGetTable()
    }

    private fun renderBuilder() {
        checkNotNull(properties.singleOrNull { it.isPk }) { "Expected exactly one @DynamoDbPartitionKey annotation on a property" }

        withBlock("public class #L {", "}", builderName) {
            properties.forEach {
                write("public var #L: #T? = null", it.name, it.typeName.asString())
            }

            withBlock("public fun build(): #T {", "}", className) {
                properties.forEach {
                    write("val #1L = requireNotNull(#1L) { #S }", it.name, "Missing value for $className.${it.name}")
                }
                blankLine()
                withBlock("return $className(", ")") {
                    properties.joinToString(", ") { it.name }
                }
            }
        }
    }

    private fun renderItemConverter() {
        withBlock("public object #L : #T<#L> by #T(", ")", converterName, Types.ItemConverter, className, Types.SimpleItemConverter) {
            write("builderFactory = ::#L", builderName)
            write("build = #L::build", builderName)
            withBlock("descriptors = #L(", ")", Types.arrayOf) {
                properties.forEach {
                    renderAttributeDescriptor(it)
                }
            }
        }
    }

    private fun renderAttributeDescriptor(prop: Property) {
        withBlock("#T(", ")", Types.AttributeDescriptor) {
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
        withBlock("public object #L : #T.#T<#L, #L> {", "}", schemaName, Types.ItemSchema, Types.PartitionKey, className, keyProperty.typeName.getShortName()) {
            write("override val converter : #1L = #1L", converterName)
            write("override val partitionKey: #T<#2L> = #2L(#S)", Types.KeySpec, keyProperty.keySpec, keyProperty.name)
        }
    }

    private val Property.keySpec: Type
        get() = when (typeName.asString()) {
            "kotlin.Int" -> Types.KeySpecNumber
            "kotlin.String" -> Types.KeySpecString
            else -> error("Unsupported key type ${typeName.asString()}, expected Int or String")
        }

    private fun renderGetTable() {
        write("public fun #T.get#2LTable(name: #T): #L.#L<#2L, #L> = #L(name, #L)",
            Types.DynamoDbMapper, className, Types.String, Types.Table, Types.PartitionKey, keyProperty.typeName.getShortName(), Types.getTable, schemaName
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


