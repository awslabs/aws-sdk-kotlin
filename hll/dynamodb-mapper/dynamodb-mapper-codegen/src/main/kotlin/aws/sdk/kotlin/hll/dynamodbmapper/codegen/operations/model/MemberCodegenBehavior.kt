/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.model.nullable
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperPkg
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.model.MapperTypes

private val attrMapTypes = setOf(MapperTypes.AttributeMap, MapperTypes.AttributeMap.nullable())
private val attrMapListTypes = Types.Kotlin.list(MapperTypes.AttributeMap).let { setOf(it, it.nullable()) }

/**
 * Describes a behavior to apply for a given [Member] in a low-level structure when generating code for an equivalent
 * high-level structure. This interface implements no behaviors on its own; it merely gives strongly-typed names to
 * behaviors that will be implemented by calling code.
 */
internal sealed interface MemberCodegenBehavior {
    /**
     * Indicates that a member should be copied as-is from a low-level structure to a high-level equivalent (i.e., no
     * changes to name, type, etc. are required)
     */
    data object PassThrough : MemberCodegenBehavior

    /**
     * Indicates that a member is an attribute map which may contain _all_ attributes for a data type (as opposed to
     * only _key_ attributes) and should be replaced with a generic type (i.e., a `Map<String, AttributeValue>` member
     * in a low-level structure should be replaced with a generic `T` member in a high-level structure)
     */
    data object MapAll : MemberCodegenBehavior

    /**
     * Indicates that a member is an attribute map which contains _key_ attributes for a data type (as opposed to _all_
     * attributes) and should be replaced with a generic type (i.e., a `Map<String, AttributeValue>` member in a
     * low-level structure should be replaced with a generic `T` member in a high-level structure)
     */
    data object MapKeys : MemberCodegenBehavior

    /**
     * Indicates that a member is a list of attribute maps which may contain attributes for a data type and should be
     * replaced with a generic list type (i.e., a `List<Map<String, AttributeValue>>` member in a low-level structure
     * should be replaced with a generic `List<T>` member in a high-level structure)
     */
    data object ListMapAll : MemberCodegenBehavior

    /**
     * Indicates that a member is unsupported and should not be replicated from a low-level structure to the high-level
     * equivalent (e.g., a deprecated member that has been replaced with new features need not be carried forward)
     */
    data object Drop : MemberCodegenBehavior

    /**
     * Indicates that a member from a low-level structure should be "hoisted" outside its high-level equivalent. This is
     * similar to [Drop] but indicates that other codegen may use the member in different ways (e.g., a table name
     * parameter in a low-level structure may be hoisted to a different API but not added to the equivalent high-level
     * structure).
     */
    data object Hoist : MemberCodegenBehavior
}

/**
 * Identifies a [MemberCodegenBehavior] for this [Member] by way of various heuristics
 */
internal val Member.codegenBehavior: MemberCodegenBehavior
    get() = when {
        this in unsupportedMembers -> MemberCodegenBehavior.Drop
        type in attrMapTypes -> if (name == "key") MemberCodegenBehavior.MapKeys else MemberCodegenBehavior.MapAll
        type in attrMapListTypes -> MemberCodegenBehavior.ListMapAll
        isTableName || isIndexName -> MemberCodegenBehavior.Hoist
        else -> MemberCodegenBehavior.PassThrough
    }

private val Member.isTableName: Boolean
    get() = name == "tableName" && type == Types.Kotlin.StringNullable

private val Member.isIndexName: Boolean
    get() = name == "indexName" && type == Types.Kotlin.StringNullable

private fun llType(name: String) = TypeRef(MapperPkg.Ll.Model, name)

private val unsupportedMembers = listOf(
    // superseded by ConditionExpression
    Member("conditionalOperator", llType("ConditionalOperator")),
    Member("expected", Types.Kotlin.stringMap(llType("ExpectedAttributeValue"))),

    // superseded by FilterExpression
    Member("queryFilter", Types.Kotlin.stringMap(llType("Condition"))),
    Member("scanFilter", Types.Kotlin.stringMap(llType("Condition"))),

    // superseded by KeyConditionExpression
    Member("keyConditions", Types.Kotlin.stringMap(llType("Condition"))),

    // superseded by ProjectionExpression
    Member("attributesToGet", Types.Kotlin.list(Types.Kotlin.String)),

    // superseded by UpdateExpression
    Member("attributeUpdates", Types.Kotlin.stringMap(llType("AttributeValueUpdate"))),

    // TODO add support for expressions
    Member("expressionAttributeNames", Types.Kotlin.stringMap(Types.Kotlin.String)),
    Member("expressionAttributeValues", MapperTypes.AttributeMap),
    Member("conditionExpression", Types.Kotlin.String),
    Member("projectionExpression", Types.Kotlin.String),
    Member("updateExpression", Types.Kotlin.String),
).map { member ->
    if (member.type is TypeRef) {
        member.copy(type = member.type.nullable())
    } else {
        member
    }
}.toSet()
