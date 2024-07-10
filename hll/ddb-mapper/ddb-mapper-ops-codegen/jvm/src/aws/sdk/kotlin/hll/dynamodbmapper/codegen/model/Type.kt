/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.Pkg
import com.google.devtools.ksp.symbol.KSTypeReference

internal sealed interface Type {
    companion object {
        fun from(ksTypeRef: KSTypeReference): TypeRef {
            val resolved = ksTypeRef.resolve()
            val name = resolved.declaration.qualifiedName!!
            return TypeRef(
                pkg = name.getQualifier(),
                shortName = name.getShortName(),
                genericArgs = resolved.arguments.map { from(it.type!!) },
                nullable = resolved.isMarkedNullable,
            )
        }

        fun list(element: Type) = TypeRef(Pkg.Kotlin.Collections, "List", listOf(element))
        fun kotlin(name: String) = TypeRef(Pkg.Kotlin.Base, name)
        fun map(key: Type, value: Type) = TypeRef(Pkg.Kotlin.Collections, "Map", listOf(key, value))
        fun stringMap(value: Type) = map(Types.String, value)
    }

    val shortName: String
    val nullable: Boolean
}

internal data class TypeRef(
    val pkg: String,
    override val shortName: String,
    val genericArgs: List<Type> = listOf(),
    override val nullable: Boolean = false,
) : Type {
    val fullName: String = "$pkg.$shortName"
}

internal data class TypeVar(override val shortName: String, override val nullable: Boolean = false) : Type

internal fun Type.nullable() = when {
    nullable -> this
    this is TypeRef -> copy(nullable = true)
    this is TypeVar -> copy(nullable = true)
    else -> error("Unknown Type ${this::class}") // Should be unreachable, only here to make compiler happy
}

internal object Types {
    // Kotlin standard types
    val String = TypeRef("kotlin", "String")
    val StringNullable = String.nullable()

    // Low-level types
    val AttributeValue = TypeRef(Pkg.Ll.Model, "AttributeValue")
    val AttributeMap = Type.map(String, AttributeValue)

    // High-level types
    val HReqContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "HReqContextImpl")
    fun itemSchema(typeVar: String) = TypeRef(Pkg.Hl.Items, "ItemSchema", listOf(TypeVar(typeVar)))
    val MapperContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "MapperContextImpl")
    val Operation = TypeRef(Pkg.Hl.PipelineImpl, "Operation")
    fun tableSpec(typeVar: String) = TypeRef(Pkg.Hl.Base, "TableSpec", listOf(TypeVar(typeVar)))
    val toItem = TypeRef(Pkg.Hl.Model, "toItem")
}
