/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.BuilderRenderer
import aws.sdk.kotlin.hll.codegen.rendering.RenderContext
import aws.sdk.kotlin.hll.codegen.rendering.RendererBase
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSortKey
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.CodegenAttributes
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

/**
 * Renders the classes and objects required to make a class usable with the DynamoDbMapper such as schemas, builders, and converters.
 * @param classDeclaration the [KSClassDeclaration] of the class
 * @param ctx the [RenderContext] of the renderer
 */
class SchemaRenderer(
    private val classDeclaration: KSClassDeclaration,
    private val ctx: RenderContext,
) : RendererBase(ctx, "${classDeclaration.qualifiedName!!.getShortName()}Schema") {
    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val classType = Type.from(classDeclaration)

    private val builderName = "${className}Builder"
    private val converterName = "${className}Converter"
    private val schemaName = "${className}Schema"

    private val properties = classDeclaration.getAllProperties().mapNotNull(AnnotatedClassProperty.Companion::from)

    init {
        check(properties.count { it.isPk } == 1) {
            "Expected exactly one @DynamoDbPartitionKey annotation on a property"
        }
        check(properties.count { it.isSk } <= 1) {
            "Expected at most one @DynamoDbSortKey annotation on a property"
        }
    }

    private val partitionKeyProp = properties.single { it.isPk }
    private val sortKeyProp = properties.singleOrNull { it.isSk }

    /**
     * We only skip rendering builders if:
     *   - the user has configured AlwaysGenerateBuilders to false (default value) AND
     *   - the class has all mutable members AND
     *   - the class has a zero-arg constructor
     */
    private val shouldRenderBuilder: Boolean = run {
        val alwaysGenerateBuilders = ctx.attributes.getOrNull(CodegenAttributes.AlwaysGenerateBuilders) ?: true
        val hasAllMutableMembers = classDeclaration.getAllProperties().map(Member.Companion::from).toSet().all { it.mutable }
        val hasZeroArgConstructor = classDeclaration.getConstructors().any { constructor -> constructor.parameters.all { it.hasDefault } }

        ctx.logger.warn("class $className: alwaysGenerateBuilders=$alwaysGenerateBuilders, hasAllMutableMembers=$hasAllMutableMembers, hasZeroArgConstructor=$hasZeroArgConstructor")
        !(!alwaysGenerateBuilders && hasAllMutableMembers && hasZeroArgConstructor)
    }

    override fun generate() {
        if (shouldRenderBuilder) { renderBuilder() }
        renderItemConverter()
        renderSchema()
        renderGetTable()
    }

    private fun renderBuilder() {
        ctx.logger.warn("Rendering a builder class for $className")
        val members = classDeclaration.getAllProperties().map(Member.Companion::from).toSet()
        BuilderRenderer(this, classType, members).render()
    }

    private fun renderItemConverter() {
        withBlock("public object #L : #T by #T(", ")", converterName, MapperTypes.Items.itemConverter(classType), MapperTypes.Items.SimpleItemConverter) {
            if (shouldRenderBuilder) {
                write("builderFactory = ::#L,", builderName)
                write("build = #L::build,", builderName)
            } else {
                write("builderFactory = { $className() },")
                write("build = { this },")
            }

            withBlock("descriptors = arrayOf(", "),") {
                properties.forEach {
                    renderAttributeDescriptor(it)
                }
            }
        }
        blankLine()
    }

    private fun renderAttributeDescriptor(prop: AnnotatedClassProperty) {
        withBlock("#T(", "),", MapperTypes.Items.AttributeDescriptor) {
            write("#S,", prop.ddbName) // key
            write("#L,", "$className::${prop.name}") // getter

            if (shouldRenderBuilder) {
                write("#L,", "$builderName::${prop.name}::set") // setter
            } else {
                write("#L,", "$className::${prop.name}::set")
            }

            write("#T", prop.valueConverter) // converter
        }
    }

    private val AnnotatedClassProperty.valueConverter: Type
        get() = when (typeName.asString()) {
            "aws.smithy.kotlin.runtime.time.Instant" -> MapperTypes.Values.DefaultInstantConverter
            "kotlin.Boolean" -> MapperTypes.Values.BooleanConverter
            "kotlin.Int" -> MapperTypes.Values.IntConverter
            "kotlin.String" -> MapperTypes.Values.StringConverter
            // TODO Add additional "standard" item converters
            else -> error("Unsupported attribute type ${typeName.asString()}")
        }

    private fun renderSchema() {
        if (sortKeyProp != null) {
            withBlock("public object #L : #T {", "}", schemaName, MapperTypes.Items.itemSchemaCompositeKey(classType, partitionKeyProp.typeRef, sortKeyProp.typeRef)) {
                write("override val converter : #1L = #1L", converterName)
                write("override val sortKey: #T = #T(#S)", MapperTypes.Items.keySpec(sortKeyProp.keySpec), sortKeyProp.keySpecType, sortKeyProp.name)
            }
        } else {
            withBlock("public object #L : #T {", "}", schemaName, MapperTypes.Items.itemSchemaPartitionKey(classType, partitionKeyProp.typeRef)) {
                write("override val converter : #1L = #1L", converterName)
                write("override val partitionKey: #T = #T(#S)", MapperTypes.Items.keySpec(partitionKeyProp.keySpec), partitionKeyProp.keySpecType, partitionKeyProp.name)
            }
        }
        blankLine()
    }

    private val AnnotatedClassProperty.keySpec: TypeRef
        get() = when (typeName.asString()) {
            "kotlin.Int" -> Types.Kotlin.Number
            "kotlin.String" -> Types.Kotlin.String
            // TODO Handle ByteArray
            else -> error("Unsupported key type ${typeName.asString()}, expected Int or String")
        }

    private val AnnotatedClassProperty.keySpecType: TypeRef
        get() = when (typeName.asString()) {
            "kotlin.Int" -> MapperTypes.Items.KeySpecNumber
            "kotlin.String" -> MapperTypes.Items.KeySpecString
            // TODO Handle ByteArray
            else -> error("Unsupported key type ${typeName.asString()}, expected Int or String")
        }

    private fun renderGetTable() {
        docs("Returns a reference to a table named [name] containing items representing [#T]", classType)

        val fnName = "get${className}Table"
        write(
            "public fun #T.#L(name: String): #T = #L(name, #L)",
            MapperTypes.DynamoDbMapper,
            fnName,
            if (sortKeyProp != null) {
                MapperTypes.Model.tableCompositeKey(classType, partitionKeyProp.typeRef, sortKeyProp.typeRef)
            } else {
                MapperTypes.Model.tablePartitionKey(classType, partitionKeyProp.typeRef)
            },
            "getTable",
            schemaName,
        )
    }
}

private data class AnnotatedClassProperty(val name: String, val typeRef: TypeRef, val ddbName: String, val typeName: KSName, val isPk: Boolean, val isSk: Boolean) {
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
                val isSk = ksProperty.isAnnotationPresent(DynamoDbSortKey::class)
                val name = ksProperty.simpleName.getShortName()
                val typeRef = Type.from(checkNotNull(ksProperty.type) { "Failed to determine class type for $name" })
                val ddbName = ksProperty.getAnnotationsByType(DynamoDbAttribute::class).singleOrNull()?.name ?: name
                AnnotatedClassProperty(name, typeRef, ddbName, typeName, isPk, isSk)
            }
    }
}
