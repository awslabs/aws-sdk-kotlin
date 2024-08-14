/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

/**
 * Describes a structure (i.e., class, struct, etc.) which contains zero or more [Member] instances
 * @param type The [TypeRef] for this structure, which includes its name and Kotlin package
 * @param members The [Member] instances which are part of this structure
 * @param attributes An [Attributes] collection for associating typed attributes with this structure
 */
data class Structure(
    val type: TypeRef,
    val members: List<Member>,
    val attributes: Attributes = emptyAttributes(),
) {
    companion object {
        /**
         * Derives a [Structure] from the given [KSTypeReference]
         */
        fun from(ksTypeRef: KSTypeReference) = Structure(
            type = Type.from(ksTypeRef),
            members = (ksTypeRef.resolve().declaration as KSClassDeclaration)
                .getDeclaredProperties()
                .map(Member.Companion::from)
                .toList(),
        )
    }
}

/**
 * Gets the low-level [Structure] equivalent for this high-level structure
 */
val Structure.lowLevel: Structure
    get() = attributes[ModelAttributes.LowLevelStructure]

/**
 * Derives a a high-level [Structure] equivalent for this low-level structure
 * @param pkg The Kotlin package to use for the high-level structure
 */
fun Structure.toHighLevel(pkg: String): Structure {
    val llStructure = this@toHighLevel

    val hlType = TypeRef(pkg, llStructure.type.shortName, listOf(TypeVar("T")))

    val hlMembers = llStructure.members.mapNotNull { llMember ->
        when (llMember.codegenBehavior) {
            MemberCodegenBehavior.PassThrough -> llMember
            MemberCodegenBehavior.MapAll, MemberCodegenBehavior.MapKeys ->
                llMember.copy(type = TypeVar("T", llMember.type.nullable))
            else -> null
        }
    }

    val hlAttributes = llStructure.attributes + (ModelAttributes.LowLevelStructure to llStructure)

    return Structure(hlType, hlMembers, hlAttributes)
}
