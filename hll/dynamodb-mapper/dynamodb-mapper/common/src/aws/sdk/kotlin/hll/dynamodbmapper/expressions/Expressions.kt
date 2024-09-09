/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.expressions

import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.*
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AndExprImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttrPathIndexImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttrPathNameImpl
import aws.sdk.kotlin.hll.dynamodbmapper.expressions.internal.AttributePathImpl
import aws.sdk.kotlin.hll.dynamodbmapper.util.attr
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Represents any kind of expression. This is an abstract top-level interface and describes no details about an
 * expression on its own. Expressions may be of various specific types (e.g., [AttributePath], [ComparisonExpr],
 * [AndExpr], etc.) each of which have specific data detailing the expression.
 *
 * [Expression] and its derivatives support the [visitor design pattern](https://en.wikipedia.org/wiki/Visitor_pattern)
 * by way of an [accept] method.
 */
public sealed interface Expression {
    /**
     * Accepts a visitor that is traversing an expression tree by dispatching to a subtype implementation. Subtype
     * implementations MUST call the [ExpressionVisitor.visit] overload for their concrete type (effectively forming a
     * [double dispatch](https://en.wikipedia.org/wiki/Double_dispatch)) and MUST return the resulting value.
     * @param visitor The [ExpressionVisitor] that is traversing an expression
     */
    public fun <T> accept(visitor: ExpressionVisitor<T>): T
}

/**
 * A subtype of [Expression] that represents a condition with a boolean value, such as would be used for filtering
 * items. This is a [marker interface](https://en.wikipedia.org/wiki/Marker_interface_pattern) which adds no additional
 * declarations.
 */
public interface BooleanExpr : Expression

/**
 * Represents an element in an [AttributePath]
 */
public sealed interface AttrPathElement {
    /**
     * Represents the name of a top-level attribute or a key in a map
     */
    public interface Name : AttrPathElement {
        /**
         * The name or key of this element
         */
        public val name: String
    }

    /**
     * Represents an index into a list/set
     */
    public interface Index : AttrPathElement {
        /**
         * The index (starting at `0`)
         */
        public val index: Int
    }
}

/**
 * Represents an expression that consists of an attribute. Attributes are referenced by attribute paths, analogous to
 * [document paths in DynamoDB](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Attributes.html#Expressions.Attributes.NestedElements.DocumentPathExamples).
 * Attribute paths consist of one or more elements, which are either names (e.g., of a top-level attribute or a nested
 * key in a map attribute) or indices (i.e., into a list). The first (and often only) element of an attribute path is a
 * name.
 *
 * See [Filter] for more information about creating references to attributes.
 */
public interface AttributePath : Expression {
    /**
     * The [AttrPathElement] for this path
     */
    public val element: AttrPathElement

    /**
     * The parent [AttributePath] (if any). If [parent] is `null` then this instance represents a top-level attribute
     * and [element] must be a name (not an index).
     */
    public val parent: AttributePath?

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [AttributePath] reference with the given name and optional parent path
 * @param name The name or key of this element
 * @param parent The parent [AttributePath] (if any) of this element. If [parent] is `null` then this instance
 * represents a top-level attribute.
 */
public fun AttributePath(name: String, parent: AttributePath? = null): AttributePath =
    AttributePathImpl(AttrPathNameImpl(name), parent)

/**
 * Creates a new [AttributePath] reference with the given index and parent path
 * @param index The index (starting at `0`) of this element
 * @param parent The parent [AttributePath] of this element
 */
public fun AttributePath(index: Int, parent: AttributePath): AttributePath =
    AttributePathImpl(AttrPathIndexImpl(index), parent)

/**
 * Represents an `AND` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `(operand[0] && operand[1] && ... && operand[n - 1])`.
 */
public interface AndExpr : BooleanExpr {
    /**
     * A list of 2 or more [BooleanExpr] conditions which are ANDed together
     */
    public val operands: List<BooleanExpr>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [AndExpr] with the given [operands]
 * @param operands A list of 2 or more [BooleanExpr] conditions which are ANDed together
 */
public fun AndExpr(operands: List<BooleanExpr>): AndExpr = AndExprImpl(operands)

/**
 * Represents a `BETWEEN` expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * This expression will be true if `value >= min && value <= max`.
 */
public interface BetweenExpr : BooleanExpr {
    /**
     * The value being compared to the [min] and [max]
     */
    public val value: Expression

    /**
     * The minimum bound for the comparison
     */
    public val min: Expression

    /**
     * The maximum bound for the comparison
     */
    public val max: Expression

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [BetweenExpr] for the given [value] and range bounded by [min] and [max]
 * @param value The value being compared to the [min] and [max]
 * @param min The minimum bound for the comparison
 * @param max The maximum bound for the comparison
 */
public fun BetweenExpr(value: Expression, min: Expression, max: Expression): BetweenExpr =
    BetweenExprImpl(value, min, max)

/**
 * Represents a function expression that yields a boolean result as described in
 * [DynamoDB's **function** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 */
public interface BooleanFuncExpr : BooleanExpr {
    /**
     * The specific boolean function to use
     */
    public val func: BooleanFunc

    /**
     * The attribute path to pass as the function's first argument
     */
    public val path: AttributePath

    /**
     * Any additional arguments used by the function
     */
    public val additionalOperands: List<Expression>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new boolean function expression
 * @param func The specific boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
public fun BooleanFuncExpr(
    func: BooleanFunc,
    path: AttributePath,
    additionalOperands: List<Expression> = listOf(),
): BooleanFuncExpr = BooleanFuncExprImpl(func, path, additionalOperands)

/**
 * Creates a new boolean function expression
 * @param func The specific boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
public fun BooleanFuncExpr(
    func: BooleanFunc,
    path: AttributePath,
    vararg additionalOperands: Expression,
): BooleanFuncExpr = BooleanFuncExprImpl(func, path, additionalOperands.toList())

/**
 * Represents a comparison expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * The specific type of comparison is identified by the [comparator] field.
 */
public interface ComparisonExpr : BooleanExpr {
    /**
     * The [Comparator] to use in the expression
     */
    public val comparator: Comparator

    /**
     * The left value being compared
     */
    public val left: Expression

    /**
     * The right value being compared
     */
    public val right: Expression

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new comparison expression
 * @param comparator The [Comparator] to use in the expression
 * @param left The left value being compared
 * @param right The right value being compared
 */
public fun ComparisonExpr(comparator: Comparator, left: Expression, right: Expression): ComparisonExpr =
    ComparisonExprImpl(comparator, left, right)

/**
 * Represents an `IN` expression as described in
 * [DynamoDB's **making comparisons** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Comparators).
 * This expression will be true if `value in set` (or, equivalently, if `set.contains(value)`).
 */
public interface InExpr : BooleanExpr {
    /**
     * The value to check for in [set]
     */
    public val value: Expression

    /**
     * The set of values to compare against [value]
     */
    public val set: Collection<Expression>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new `IN` expression
 * @param value The value to check for in [set]
 * @param set The set of values to compare against [value]
 */
public fun InExpr(value: Expression, set: Collection<Expression>): InExpr = InExprImpl(value, set)

/**
 * Represents an expression that consists of a single literal value
 */
public interface LiteralExpr : Expression {
    /**
     * The low-level DynamoDB representation of the literal value
     */
    public val value: AttributeValue

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new literal expression
 * @param value The low-level DynamoDB representation of the literal value
 */
public fun LiteralExpr(value: AttributeValue): LiteralExpr = LiteralExprImpl(value)

private val NULL_LITERAL = LiteralExpr(attr(null))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: Boolean?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: ByteArray?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: List<Any?>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: Map<String, Any?>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@Suppress("UNUSED_PARAMETER")
public fun LiteralExpr(value: Nothing?): LiteralExpr = NULL_LITERAL

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: Number?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetByteArray")
public fun LiteralExpr(value: Set<ByteArray>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetNumber")
public fun LiteralExpr(value: Set<Number>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetString")
public fun LiteralExpr(value: Set<String>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetUByte")
public fun LiteralExpr(value: Set<UByte>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetUInt")
public fun LiteralExpr(value: Set<UInt>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetULong")
public fun LiteralExpr(value: Set<ULong>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
@JvmName("LiteralExprSetUShort")
public fun LiteralExpr(value: Set<UShort>?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: String?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: UByte?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: UInt?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: ULong?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Creates a new literal expression
 * @param value The literal value which will be converted to an [AttributeValue]
 */
public fun LiteralExpr(value: UShort?): LiteralExpr = LiteralExpr(attr(value))

/**
 * Represents a `NOT` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `!operand` (i.e., `operand` evaluates to `false`).
 */
public interface NotExpr : BooleanExpr {
    /**
     * The condition to negate
     */
    public val operand: BooleanExpr

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new `NOT` expression
 * @param operand The condition to negate
 */
public fun NotExpr(operand: BooleanExpr): NotExpr = NotExprImpl(operand)

/**
 * Represents an `OR` expression as described in
 * [DynamoDB's **logical evaluations** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.LogicalEvaluations).
 * This expression will be true if `(operand[0] || operand[1] || ... || operand[n - 1])`.
 */
public interface OrExpr : BooleanExpr {
    /**
     * A list of 2 or more [BooleanExpr] conditions which are ANDed together
     */
    public val operands: List<BooleanExpr>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new [OrExpr] with the given [operands]
 * @param operands A list of 2 or more [BooleanExpr] conditions which are ORed together
 */
public fun OrExpr(operands: List<BooleanExpr>): OrExpr = OrExprImpl(operands)

/**
 * Represents a function expression that yields a non-boolean result as described in
 * [DynamoDB's **function** documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.OperatorsAndFunctions.html#Expressions.OperatorsAndFunctions.Functions)
 */
public interface ScalarFuncExpr : BooleanExpr {
    /**
     * The specific non-boolean function to use
     */
    public val func: ScalarFunc

    /**
     * The attribute path to pass as the function's first argument
     */
    public val path: AttributePath

    /**
     * Any additional arguments used by the function
     */
    public val additionalOperands: List<Expression>

    override fun <T> accept(visitor: ExpressionVisitor<T>): T = visitor.visit(this)
}

/**
 * Creates a new non-boolean function expression
 * @param func The specific non-boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
public fun ScalarFuncExpr(
    func: ScalarFunc,
    path: AttributePath,
    additionalOperands: List<Expression> = listOf(),
): ScalarFuncExpr = ScalarFuncExprImpl(func, path, additionalOperands)

/**
 * Creates a new non-boolean function expression
 * @param func The specific non-boolean function to use
 * @param path The attribute path to pass as the function's first argument
 * @param additionalOperands Any additional arguments used by the function
 */
public fun ScalarFuncExpr(
    func: ScalarFunc,
    path: AttributePath,
    vararg additionalOperands: Expression,
): ScalarFuncExpr = ScalarFuncExprImpl(func, path, additionalOperands.toList())
