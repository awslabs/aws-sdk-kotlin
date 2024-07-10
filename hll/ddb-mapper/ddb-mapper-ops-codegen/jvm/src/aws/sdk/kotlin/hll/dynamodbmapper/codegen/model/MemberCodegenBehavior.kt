/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.Pkg

private val attrMapTypes = setOf(Types.AttributeMap, Types.AttributeMap.nullable())

internal sealed interface MemberCodegenBehavior {
    companion object {
        fun identifyFor(member: Member) = when {
            member in unsupportedMembers -> Drop
            member.type in attrMapTypes -> if (member.name == "key") MapKeys else MapAll
            member.isTableName -> Hoist
            else -> PassThrough
        }.also { println("  ${member.name} is $it") }
    }

    data object PassThrough : MemberCodegenBehavior
    data object MapAll : MemberCodegenBehavior
    data object MapKeys : MemberCodegenBehavior
    data object Drop : MemberCodegenBehavior
    data object Hoist : MemberCodegenBehavior // FIXME Note sure this is useful...get rid of Hoist?
}

internal val Member.codegenBehavior: MemberCodegenBehavior
    get() = MemberCodegenBehavior.identifyFor(this)

private val Member.isTableName: Boolean
    get() = name == "tableName" && type == Types.StringNullable

private fun llType(name: String) = TypeRef(Pkg.Ll.Model, name)

private val unsupportedMembers = listOf(
    // superseded by ConditionExpression
    Member("conditionalOperator", llType("ConditionalOperator")),
    Member("expected", Type.stringMap(llType("ExpectedAttributeValue"))),

    // superseded by FilterExpression
    Member("queryFilter", Type.stringMap(llType("Condition"))),
    Member("scanFilter", Type.stringMap(llType("Condition"))),

    // superseded by KeyConditionExpression
    Member("keyConditions", Type.stringMap(llType("Condition"))),

    // superseded by ProjectionExpression
    Member("attributesToGet", Type.list(Types.String)),

    // superseded by UpdateExpression
    Member("attributeUpdates", Type.stringMap(llType("AttributeValueUpdate"))),

    // TODO add support for expressions
    Member("expressionAttributeNames", Type.stringMap(Types.String)),
    Member("expressionAttributeValues", Types.AttributeMap),
    Member("conditionExpression", Types.String),
    Member("projectionExpression", Types.String),
    Member("updateExpression", Types.String),
).map { member ->
    if (member.type is TypeRef) {
        member.copy(type = member.type.nullable())
    } else {
        member
    }
}.toSet()
