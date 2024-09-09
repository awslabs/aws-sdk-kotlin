/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.*
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.LiteralExpr
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Represents the name of a top-level attribute or a key in a map
 * @param name The name or key
 */
internal data class AttrPathNameImpl(override val name: String) : AttrPathElement.Name

/**
 * Represents an index into a list/set
 * @param index The index (starting at `0`)
 */
internal data class AttrPathIndexImpl(override val index: Int) : AttrPathElement.Index

/**
 * Represents an expression that consists of an attribute path
 * @param element The [AttrPathElement] for this path
 * @param parent The parent [AttributePath] (if any). If [parent] is `null` then this instance represents a top-level
 * attribute and [element] must be a name (not an index). Defaults to `null`.
 */
internal data class AttributePathImpl(
    override val element: AttrPathElement,
    override val parent: AttributePath? = null,
) : AttributePath {
    init {
        require(element is AttrPathElement.Name || parent != null) {
            "Top-level attribute paths must be names (not indices)"
        }
    }
}

/**
 * Represents an `AND` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `(operand[0] && operand[1] && ... && operand[n - 1])`.
 * @param operands A list of 2 or more [BooleanExpr] conditions which are ANDed together
 */
internal data class AndExprImpl(override val operands: List<BooleanExpr>) : AndExpr {
    init {
        require(operands.size > 1) { "AND operations require two or more operands" }
    }
}

/**
 * Represents a `BETWEEN` expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * This expression will be true if `value >= min && value <= max`.
 * @param value The value being compared to the [min] and [max]
 * @param min The minimum bound for the comparison
 * @param max The maximum bound for the comparison
 */
internal data class BetweenExprImpl(
    override val value: Expression,
    override val min: Expression,
    override val max: Expression,
) : BetweenExpr

/**
 * Represents a function expression that yields a boolean result as described in
 * [DynamoDB's **function** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 * @param func The specific boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
internal data class BooleanFuncExprImpl(
    override val func: BooleanFunc,
    override val path: AttributePath,
    override val additionalOperands: List<Expression> = listOf(),
) : BooleanFuncExpr

/**
 * Represents a comparison expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * The specific type of comparison is identified by the [comparator] field.
 * @param comparator The [Comparator] to use in the expression
 * @param left The left value being compared
 * @param right The right value being compared
 */
internal data class ComparisonExprImpl(
    override val comparator: Comparator,
    override val left: Expression,
    override val right: Expression,
) : ComparisonExpr

/**
 * Represents an `IN` expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * This expression will be true if `value in set` (or, equivalently, if `set.contains(value)`).
 * @param value The value to check for in [set]
 * @param set The set of values to compare against [value]
 */
internal data class InExprImpl(override val value: Expression, override val set: Collection<Expression>) : InExpr

/**
 * Represents an expression that consists of a single literal value
 * @param value The low-level DynamoDB representation of the literal value
 */
internal data class LiteralExprImpl(override val value: AttributeValue) : LiteralExpr

/**
 * Represents a `NOT` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `!operand` (i.e., `operand` evaluates to `false`).
 * @param operand The condition to negate
 */
internal data class NotExprImpl(override val operand: BooleanExpr) : NotExpr

/**
 * Represents an `OR` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `(operand[0] || operand[1] || ... || operand[n - 1])`.
 * @param operands A list of 2 or more [BooleanExpr] conditions which are ORed together
 */
internal data class OrExprImpl(override val operands: List<BooleanExpr>) : OrExpr {
    init {
        require(operands.size > 1) { "OR operations require two or more operands" }
    }
}

/**
 * Represents a function expression that yields a non-boolean result as described in
 * [DynamoDB's **function** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 * @param func The specific boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
internal data class ScalarFuncExprImpl(
    override val func: ScalarFunc,
    override val path: AttributePath,
    override val additionalOperands: List<Expression> = listOf(),
) : ScalarFuncExpr
