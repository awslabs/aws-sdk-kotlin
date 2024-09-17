/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.*
import aws.sdk.kotlin.hll.codegen.util.plus
import aws.smithy.kotlin.runtime.collections.get

/**
 * Gets the low-level [Structure] equivalent for this high-level structure
 */
internal val Structure.lowLevel: Structure
    get() = attributes[ModelAttributes.LowLevelStructure]

/**
 * Derives a high-level [Structure] equivalent for this low-level structure
 * @param pkg The Kotlin package to use for the high-level structure
 */
internal fun Structure.toHighLevel(pkg: String): Structure {
    val llStructure = this@toHighLevel

    val hlType = TypeRef(pkg, llStructure.type.shortName, listOf(TypeVar("T")))

    val hlMembers = llStructure.members.mapNotNull { llMember ->
        when (llMember.codegenBehavior) {
            MemberCodegenBehavior.PassThrough -> llMember

            MemberCodegenBehavior.MapAll, MemberCodegenBehavior.MapKeys ->
                llMember.copy(type = TypeVar("T", llMember.type.nullable))

            MemberCodegenBehavior.ListMapAll -> {
                val llListType = llMember.type as? TypeRef ?: error("`ListMapAll` member is required to be a TypeRef")
                val hlListType = llListType.copy(genericArgs = listOf(TypeVar("T")), nullable = llListType.nullable)
                llMember.copy(type = hlListType)
            }

            else -> null
        }
    }.toSet()

    val hlAttributes = llStructure.attributes + (ModelAttributes.LowLevelStructure to llStructure)

    return Structure(hlType, hlMembers, hlAttributes)
}
