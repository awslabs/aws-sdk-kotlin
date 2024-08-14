/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.core.ImportDirective
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.DynamoDbMapperTypes
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Renders the classes and objects required to make a class usable with the DynamoDbMapper such as schemas, builders, and converters.
 * @param classDeclaration the [KSClassDeclaration] of the class
 * @param ctx the [RenderContext] of the renderer
 */
public class SchemaRenderer(
    private val classDeclaration: KSClassDeclaration,
    private val ctx: RenderContext,
) : RendererBase(ctx, "${classDeclaration.qualifiedName!!.getShortName()}Schema", "dynamodb-mapper-annotation-processor") {
    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val classType = Type.from(
        checkNotNull(classDeclaration.primaryConstructor?.returnType) {
            "Failed to determine class type for $className"
        },
    )

    private val builderName = "${className}Builder"
    private val converterName = "${className}Converter"
    private val schemaName = "${className}Schema"

    private val properties = classDeclaration.getAllProperties().mapNotNull(AnnotatedClassProperty.Companion::from)
    private val keyProperty = checkNotNull(properties.singleOrNull { it.isPk }) {
        "Expected exactly one @DynamoDbPartitionKey annotation on a property"
    }

    override fun generate() {
        imports.add(ImportDirective(classDeclaration.qualifiedName!!.asString()))
        renderBuilder()
        renderItemConverter()
        renderSchema()
        renderGetTable()
    }

    // TODO Not all classes need builders generated (i.e. the class consists of all public mutable members), add configurability here
    private fun renderBuilder() = BuilderRenderer(this, classDeclaration).render()

    private fun renderItemConverter() {
        withBlock("public object #L : #T<#L> by #T(", ")", converterName, DynamoDbMapperTypes.ItemConverter, className, DynamoDbMapperTypes.SimpleItemConverter) {
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

    private fun renderAttributeDescriptor(prop: AnnotatedClassProperty) {
        withBlock("#T(", "),", DynamoDbMapperTypes.AttributeDescriptor) {
            write("#S,", prop.ddbName) // key
            write("#L,", "$className::${prop.name}") // getter
            write("#L,", "$builderName::${prop.name}::set") // setter
            write("#T", prop.valueConverter) // converter
        }
    }

    private val AnnotatedClassProperty.valueConverter: Type
        get() = when (typeName.asString()) {
            "aws.smithy.kotlin.runtime.time.Instant" -> DynamoDbMapperTypes.DefaultInstantConverter
            "kotlin.Boolean" -> DynamoDbMapperTypes.BooleanConverter
            "kotlin.Int" -> DynamoDbMapperTypes.IntConverter
            "kotlin.String" -> DynamoDbMapperTypes.StringConverter
            // TODO Add additional "standard" item converters
            else -> error("Unsupported attribute type ${typeName.asString()}")
        }

    private fun renderSchema() {
        withBlock("public object #L : #T.#L<#L, #L> {", "}", schemaName, DynamoDbMapperTypes.ItemSchema, "PartitionKey", className, keyProperty.typeName.getShortName()) {
            write("override val converter : #1L = #1L", converterName)
            // TODO Handle composite keys
            write("override val partitionKey: #1T<#2L> = #1T.#2L(#3S)", DynamoDbMapperTypes.KeySpec, keyProperty.keySpec, keyProperty.name)
        }
        blankLine()
    }

    private val AnnotatedClassProperty.keySpec: String
        get() = when (typeName.asString()) {
            "kotlin.Int" -> "Number"
            "kotlin.String" -> "String"
            // TODO Handle ByteArray
            else -> error("Unsupported key type ${typeName.asString()}, expected Int or String")
        }

    private fun renderGetTable() {
        withDocs {
            write("Returns a reference to a table named [name] containing items representing [#L]", className)
        }

        val fnName = "get${className}Table"
        write(
            "public fun #T.#L(name: String): #T = #L(name, #L)",
            DynamoDbMapperTypes.DynamoDbMapper,
            fnName,
            DynamoDbMapperTypes.tablePartitionKey(classType, keyProperty.typeRef),
            "getTable",
            schemaName
        )
    }
}

private data class AnnotatedClassProperty(val name: String, val typeRef: TypeRef, val ddbName: String, val typeName: KSName, val isPk: Boolean) {
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
                val typeRef = Type.from(checkNotNull(ksProperty.type) { "Failed to determine class type for $name" })
                val ddbName = ksProperty.getAnnotationsByType(DynamoDbAttribute::class).singleOrNull()?.name ?: name
                AnnotatedClassProperty(name, typeRef, ddbName, typeName, isPk)
            }
    }
}
