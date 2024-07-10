/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.plus
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

internal data class Structure(
    val type: Type,
    val members: List<Member>,
    val attributes: Attributes = emptyAttributes(),
) {
    companion object {
        fun from(ksTypeRef: KSTypeReference) = Structure(
            type = Type.from(ksTypeRef),
            members = (ksTypeRef.resolve().declaration as KSClassDeclaration)
                .getDeclaredProperties()
                .map(Member::from)
                .toList(),
        )
    }
}

internal val Structure.lowLevel: Structure
    get() = attributes[ModelAttributes.LowLevelStructure]

internal fun Structure.toHighLevel(pkg: String): Structure {
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

    val hAttributes = llStructure.attributes + (ModelAttributes.LowLevelStructure to llStructure)

    return Structure(hlType, hlMembers, hAttributes)
}
