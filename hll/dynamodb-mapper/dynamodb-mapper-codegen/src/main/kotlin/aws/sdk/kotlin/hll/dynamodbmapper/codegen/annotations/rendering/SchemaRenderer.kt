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
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbAttribute
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbPartitionKey
import aws.sdk.kotlin.hll.dynamodbmapper.DynamoDbSortKey
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes
import aws.smithy.kotlin.runtime.collections.get
import aws.smithy.kotlin.runtime.util.type
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

    private val properties = classDeclaration.getAllProperties().filterNot { it.modifiers.contains(Modifier.PRIVATE) }

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

    override fun generate() {
        if (shouldRenderBuilder) {
            renderBuilder()
        }
        renderItemConverter()
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
                properties.forEach {
                    renderAttributeDescriptor(it)
                }
            }
        }
        blankLine()
    }

    private fun renderAttributeDescriptor(prop: KSPropertyDeclaration) {
        withBlock("#T(", "),", MapperTypes.Items.AttributeDescriptor) {
            write("#S,", prop.ddbName) // key
            write("#L,", "$className::${prop.name}") // getter

            // setter
            if (shouldRenderBuilder) {
                write("#L,", "$builderName::${prop.name}::set")
            } else {
                write("#L,", "$className::${prop.name}::set")
            }

            when (prop.typeName) {
                "kotlin.collections.List" -> write("#T(#T)", MapperTypes.Values.Collections.ListConverter, prop.valueConverter)
                "kotlin.collections.Map" -> {
                    check(prop.typeRef.genericArgs.size == 2) { "Expected map type $prop to have 2 generic args, got ${prop.typeRef.genericArgs.size}" }
                }
                else -> write("#T", prop.valueConverter)
            }
        }
    }

    private val KSPropertyDeclaration.valueConverter: Type
        get() = when (typeName) {
            "aws.smithy.kotlin.runtime.time.Instant" -> MapperTypes.Values.SmithyTypes.DefaultInstantConverter
            "aws.smithy.kotlin.runtime.net.url.Url" -> MapperTypes.Values.SmithyTypes.UrlConverter
            "aws.smithy.kotlin.runtime.content.Document" -> MapperTypes.Values.SmithyTypes.DefaultDocumentConverter

            "kotlin.Boolean" -> MapperTypes.Values.Scalars.BooleanConverter

            "kotlin.String" -> MapperTypes.Values.Scalars.StringConverter
            "kotlin.CharArray" -> MapperTypes.Values.Scalars.CharArrayConverter
            "kotlin.Char" -> MapperTypes.Values.Scalars.CharConverter

            "kotlin.Byte" -> MapperTypes.Values.Scalars.ByteConverter
            "kotlin.ByteArray" -> MapperTypes.Values.Scalars.ByteArrayConverter
            "kotlin.Short" -> MapperTypes.Values.Scalars.ShortConverter
            "kotlin.Int" -> MapperTypes.Values.Scalars.IntConverter
            "kotlin.Long" -> MapperTypes.Values.Scalars.LongConverter
            "kotlin.Double" -> MapperTypes.Values.Scalars.DoubleConverter
            "kotlin.Float" -> MapperTypes.Values.Scalars.FloatConverter

            "kotlin.UByte" -> MapperTypes.Values.Scalars.UByteConverter
            "kotlin.UInt" -> MapperTypes.Values.Scalars.UIntConverter
            "kotlin.UShort" -> MapperTypes.Values.Scalars.UShortConverter
            "kotlin.ULong" -> MapperTypes.Values.Scalars.ULongConverter

            // FIXME Should this check the full element name (kotlin.String) instead of just String
            // genericArgs don't seem structured to provide a full package name (i.e what is the package in List<T>?)
            "kotlin.collections.List" -> when (val listElementName = this.typeRef.genericArgs.single().shortName) {
                "String" -> MapperTypes.Values.Scalars.StringConverter
                "CharArray" -> MapperTypes.Values.Scalars.CharArrayConverter
                "Char" -> MapperTypes.Values.Scalars.CharConverter

                "Byte" -> MapperTypes.Values.Scalars.ByteConverter
                "ByteArray" -> MapperTypes.Values.Scalars.ByteArrayConverter
                "Short" -> MapperTypes.Values.Scalars.ShortConverter
                "Int" -> MapperTypes.Values.Scalars.IntConverter
                "Long" -> MapperTypes.Values.Scalars.LongConverter
                "Double" -> MapperTypes.Values.Scalars.DoubleConverter
                "Float" -> MapperTypes.Values.Scalars.FloatConverter

                "UByte" -> MapperTypes.Values.Scalars.UByteConverter
                "UInt" -> MapperTypes.Values.Scalars.UIntConverter
                "UShort" -> MapperTypes.Values.Scalars.UShortConverter
                "ULong" -> MapperTypes.Values.Scalars.ULongConverter
                // FIXME Handle custom list elements (e.g. List<Foo>)

                else -> error("Unsupported list element $listElementName")
            }

            // FIXME Should this check the full element name (kotlin.String) instead of just String
            "kotlin.collections.Set" -> when (val setElementName = this.typeRef.genericArgs.single().shortName) {
                "String" -> MapperTypes.Values.Collections.StringSetConverter
                "Char" -> MapperTypes.Values.Collections.CharSetConverter
                "CharArray" -> MapperTypes.Values.Collections.CharArraySetConverter

                "Byte" -> MapperTypes.Values.Collections.ByteSetConverter
                "Double" -> MapperTypes.Values.Collections.DoubleSetConverter
                "Float" -> MapperTypes.Values.Collections.FloatSetConverter
                "Int" -> MapperTypes.Values.Collections.IntSetConverter
                "Long" -> MapperTypes.Values.Collections.LongSetConverter
                "Short" -> MapperTypes.Values.Collections.ShortSetConverter
                "UByte" -> MapperTypes.Values.Collections.UByteSetConverter
                "UInt" -> MapperTypes.Values.Collections.UIntSetConverter
                "ULong" -> MapperTypes.Values.Collections.ULongSetConverter
                "UShort" -> MapperTypes.Values.Collections.UShortSetConverter

                // FIXME Handle custom set elements (e.g. Set<Foo>)

                else -> error("Unsupported set element $setElementName")
            }

            else -> error("Unsupported attribute type $typeName")
        }

    private fun renderSchema() {
        val schemaType = if (sortKeyProp != null) {
            MapperTypes.Items.itemSchemaCompositeKey(classType, partitionKeyProp.typeRef, sortKeyProp.typeRef)
        } else {
            MapperTypes.Items.itemSchemaPartitionKey(classType, partitionKeyProp.typeRef)
        }

        withBlock("#Lobject #L : #T {", "}", ctx.attributes.visibility, schemaName, schemaType) {
            write("override val converter : #1L = #1L", converterName)
            write("override val partitionKey: #T = #T(#S)", MapperTypes.Items.keySpec(partitionKeyProp.keySpec), partitionKeyProp.keySpecType, partitionKeyProp.name)
            if (sortKeyProp != null) {
                write("override val sortKey: #T = #T(#S)", MapperTypes.Items.keySpec(sortKeyProp.keySpec), sortKeyProp.keySpecType, sortKeyProp.name)
            }
        }
        blankLine()
    }

    private val KSPropertyDeclaration.keySpec: TypeRef
        get() = when (typeName) {
            "kotlin.ByteArray" -> Types.Kotlin.ByteArray
            "kotlin.Int" -> Types.Kotlin.Number
            "kotlin.String" -> Types.Kotlin.String
            else -> error("Unsupported key type $typeName, expected ByteArray, Int, or String")
        }

    private val KSPropertyDeclaration.keySpecType: TypeRef
        get() = when (typeName) {
            "kotlin.ByteArray" -> MapperTypes.Items.KeySpecByteArray
            "kotlin.Int" -> MapperTypes.Items.KeySpecNumber
            "kotlin.String" -> MapperTypes.Items.KeySpecString
            else -> error("Unsupported key type $typeName, expected ByteArray, Int, or String")
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

private val KSPropertyDeclaration.typeName: String
    get() = checkNotNull(getter?.returnType?.resolve()?.declaration?.qualifiedName?.asString()) { "Failed to determine type name for $this" }

@OptIn(KspExperimental::class)
private val KSPropertyDeclaration.isPk: Boolean
    get() = isAnnotationPresent(DynamoDbPartitionKey::class)

@OptIn(KspExperimental::class)
private val KSPropertyDeclaration.isSk: Boolean
    get() = isAnnotationPresent(DynamoDbSortKey::class)

private val KSPropertyDeclaration.name: String
    get() = simpleName.getShortName()

private val KSPropertyDeclaration.typeRef: TypeRef
    get() = Type.from(checkNotNull(type) { "Failed to determine class type for $name" })

@OptIn(KspExperimental::class)
private val KSPropertyDeclaration.ddbName: String
    get() = getAnnotationsByType(DynamoDbAttribute::class).singleOrNull()?.name ?: name