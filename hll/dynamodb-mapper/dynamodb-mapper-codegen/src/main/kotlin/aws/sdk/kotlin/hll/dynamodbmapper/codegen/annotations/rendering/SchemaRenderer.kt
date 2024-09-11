/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.rendering

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.rendering.*
import aws.sdk.kotlin.hll.codegen.util.visibility
import aws.sdk.kotlin.hll.dynamodbmapper.*
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

/**
 * Renders the classes and objects required to make a class usable with the DynamoDbMapper such as schemas, builders, and converters.
 * @param classDeclaration the [KSClassDeclaration] of the class
 * @param ctx the [RenderContext] of the renderer
 */
internal class SchemaRenderer(
    private val classDeclaration: KSClassDeclaration,
    private val ctx: RenderContext,
) : RendererBase(ctx, "${classDeclaration.qualifiedName!!.getShortName()}Schema") {
    private val className = classDeclaration.qualifiedName!!.getShortName()
    private val classType = Type.from(classDeclaration)

    private val builderName = "${className}Builder"
    private val converterName = "${className}Converter"
    private val schemaName = "${className}Schema"

    @OptIn(KspExperimental::class)
    private val properties = classDeclaration.getAllProperties()
        .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
        .filterNot { it.isAnnotationPresent(DynamoDbIgnore::class) }

    private val annotatedProperties = properties.mapNotNull(AnnotatedClassProperty.Companion::from)

    init {
        check(annotatedProperties.count { it.isPk } == 1) {
            "Expected exactly one @DynamoDbPartitionKey annotation on a property"
        }
        check(annotatedProperties.count { it.isSk } <= 1) {
            "Expected at most one @DynamoDbSortKey annotation on a property"
        }
    }

    private val partitionKeyProp = annotatedProperties.single { it.isPk }
    private val sortKeyProp = annotatedProperties.singleOrNull { it.isSk }

    @OptIn(KspExperimental::class)
    private val itemConverter: Type = run {
            val qualifiedName = classDeclaration.getAnnotationsByType(DynamoDbItemConverter::class).singleOrNull()?.qualifiedName

            qualifiedName?.let {
                val pkg = it.substringBeforeLast(".")
                val shortName = it.removePrefix("$pkg.")
                TypeRef(pkg, shortName)
            } ?: TypeRef(ctx.pkg, converterName)
        }

    /**
     * We skip rendering a class builder if:
     *   - the user has configured GenerateBuilders to WHEN_REQUIRED (default value) AND
     *   - the class has all mutable members AND
     *   - the class has a zero-arg constructor
     */
    private val shouldRenderBuilder: Boolean = run {
        val alwaysGenerateBuilders = ctx.attributes[AnnotationsProcessorOptions.GenerateBuilderClassesAttribute] == GenerateBuilderClasses.ALWAYS
        val hasAllMutableMembers = properties.all { it.isMutable }
        val hasZeroArgConstructor = classDeclaration.getConstructors().any { constructor -> constructor.parameters.all { it.hasDefault } }

        !(!alwaysGenerateBuilders && hasAllMutableMembers && hasZeroArgConstructor)
    }

    @OptIn(KspExperimental::class)
    override fun generate() {
        if (shouldRenderBuilder) {
            renderBuilder()
        }

        if (!classDeclaration.isAnnotationPresent(DynamoDbItemConverter::class)) {
            renderItemConverter()
        }

        renderSchema()

        if (ctx.attributes[AnnotationsProcessorOptions.GenerateGetTableMethodAttribute]) {
            renderGetTable()
        }
    }

    private fun renderBuilder() {
        val members = properties.map(Member.Companion::from).toSet()
        BuilderRenderer(this, classType, members, ctx).render()
    }

    private fun renderItemConverter() {
        withBlock("#Lobject #L : #T by #T(", ")", ctx.attributes.visibility, converterName, MapperTypes.Items.itemConverter(classType), MapperTypes.Items.SimpleItemConverter) {
            if (shouldRenderBuilder) {
                write("builderFactory = ::#L,", builderName)
                write("build = #L::build,", builderName)
            } else {
                write("builderFactory = { $className() },")
                write("build = { this },")
            }

            withBlock("descriptors = arrayOf(", "),") {
                annotatedProperties.forEach {
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

            // setter
            if (shouldRenderBuilder) {
                write("#L,", "$builderName::${prop.name}::set")
            } else {
                write("#L,", "$className::${prop.name}::set")
            }

            write("#T", prop.valueConverter) // converter
        }
    }

    private val AnnotatedClassProperty.valueConverter: Type
        get() = when (typeName.asString()) {
            "aws.smithy.kotlin.runtime.time.Instant" -> MapperTypes.Values.SmithyTypes.DefaultInstantConverter
            "kotlin.Boolean" -> MapperTypes.Values.Scalars.BooleanConverter
            "kotlin.Int" -> MapperTypes.Values.Scalars.IntConverter
            "kotlin.String" -> MapperTypes.Values.Scalars.StringConverter
            // TODO Add additional "standard" item converters
            else -> error("Unsupported attribute type ${typeName.asString()}")
        }

    private fun renderSchema() {
        val schemaType = if (sortKeyProp != null) {
            MapperTypes.Items.itemSchemaCompositeKey(classType, partitionKeyProp.typeRef, sortKeyProp.typeRef)
        } else {
            MapperTypes.Items.itemSchemaPartitionKey(classType, partitionKeyProp.typeRef)
        }

        withBlock("#Lobject #L : #T {", "}", ctx.attributes.visibility, schemaName, schemaType) {
            write("override val converter : #1T = #1T", itemConverter)
            write("override val partitionKey: #T = #T(#S)", MapperTypes.Items.keySpec(partitionKeyProp.keySpec), partitionKeyProp.keySpecType, partitionKeyProp.name)
            if (sortKeyProp != null) {
                write("override val sortKey: #T = #T(#S)", MapperTypes.Items.keySpec(sortKeyProp.keySpec), sortKeyProp.keySpecType, sortKeyProp.name)
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
            "#Lfun #T.#L(name: String): #T = #L(name, #L)",
            ctx.attributes.visibility,
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
