/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Describes a Kotlin data type
 */
sealed interface Type {
    companion object {
        /**
         * Derives a [TypeRef] from a [KSClassDeclaration]
         */
        fun from(ksClassDeclaration: KSClassDeclaration): TypeRef = from(ksClassDeclaration.asStarProjectedType())

        /**
         * Derives a [TypeRef] from a [KSTypeReference]
         */
        fun from(ksTypeRef: KSTypeReference): TypeRef = from(ksTypeRef.resolve())

        /**
         * Derives a [TypeRef] from a [KSType]
         */
        fun from(ksType: KSType): TypeRef {
            val name = ksType.declaration.qualifiedName!!
            return TypeRef(
                pkg = name.getQualifier(),
                shortName = name.getShortName(),
                genericArgs = ksType.arguments.map { from(it.type!!) },
                nullable = ksType.isMarkedNullable,
            )
        }
    }

    /**
     * Gets the short name (i.e., not including the Kotlin package) for this type
     */
    val shortName: String

    /**
     * Indicates whether instances of this type allow nullable references
     */
    val nullable: Boolean
}

/**
 * A reference to a specific, named type (e.g., [kotlin.String]).
 *
 * This type reference may have generic arguments, which are themselves instances of a [Type]. For instance, a [TypeRef]
 * representing [kotlin.collections.List] would have a single generic argument, which may either be a concrete [TypeRef]
 * itself (e.g., `List<String>`) or a generic [TypeVar] (e.g., `List<T>`).
 * @param pkg The Kotlin package for this type
 * @param shortName The short name (i.e., not including the kotlin package) for this type
 * @param genericArgs Zero or more [Type] generic arguments to this type
 * @param nullable Indicates whether instances of this type allow nullable references
 */
data class TypeRef(
    val pkg: String,
    override val shortName: String,
    val genericArgs: List<Type> = listOf(),
    override val nullable: Boolean = false,
) : Type {
    /**
     * The full name of this type, including the Kotlin package
     */
    val fullName: String = "$pkg.$shortName"
}

/**
 * A generic type variable (e.g., `T`)
 * @param shortName The name of this type variable
 * @param nullable Indicates whether instances of this type allow nullable references
 */
data class TypeVar(override val shortName: String, override val nullable: Boolean = false) : Type

/**
 * Derives a nullable [Type] equivalent for this type
 */
public fun Type.nullable() = when {
    nullable -> this
    this is TypeRef -> copy(nullable = true)
    this is TypeVar -> copy(nullable = true)
    else -> error("Unknown Type ${this::class}") // Should be unreachable, only here to make compiler happy
}
