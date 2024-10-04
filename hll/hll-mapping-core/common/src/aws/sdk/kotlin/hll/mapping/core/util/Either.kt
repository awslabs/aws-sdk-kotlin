/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.mapping.core.util

import aws.smithy.kotlin.runtime.ExperimentalApi

/**
 * Represents a value which may be one of two possible types: [L] or [R]. An instance of this type will be either [Left]
 * or [Right].
 *
 * By convention [Either] is **right-biased**, meaning that [Right] values are the default values to operate on (e.g.,
 * via [map]) and [Left] value are typically unmodified. This lends itself to using [R]/[Right] for values which may
 * require more processing and using [L]/[Left] for values which are relatively "final".
 *
 * @param L The type of [Left] values
 * @param R The type of [Right] values
 */
@ExperimentalApi
public sealed interface Either<out L, out R> {
    /**
     * The left side of an [Either]
     * @param L The type of values held in this class
     */
    @ExperimentalApi
    public interface Left<out L> : Either<L, Nothing> {
        /**
         * An [L] value
         */
        public val value: L
    }

    /**
     * The right side of an [Either]
     * @param R The type of values held in this class
     */
    @ExperimentalApi
    public interface Right<out R> : Either<Nothing, R> {
        /**
         * An [R] value
         */
        public val value: R
    }

    @ExperimentalApi
    public companion object {
        /**
         * Creates a new [Left] with the given [value]
         * @param L The type of values held in this class
         * @param value An [L] value
         */
        public fun <L> Left(value: L): Left<L> = LeftImpl(value)

        /**
         * Creates a new [Right] with the given [value]
         * @param R The type of values held in this class
         * @param value An [R] value
         */
        public fun <R> Right(value: R): Right<R> = RightImpl(value)
    }
}

private data class LeftImpl<out L>(override val value: L) : Either.Left<L>

private data class RightImpl<out R>(override val value: R) : Either.Right<R>

/**
 * Map the right value of this [Either] to a new value. Left values are unmodified.
 * @param L The type of left value
 * @param R The current type of right value
 * @param R2 The new type of right value
 * @param func A mapping function which turns an [R] into an [R2]
 */
@ExperimentalApi
public inline fun <L, R, R2> Either<L, R>.map(func: (right: R) -> R2): Either<L, R2> = when (this) {
    is Either.Left -> this
    is Either.Right -> Either.Right(func(value))
}

/**
 * Transform this [Either] into a value of type [T] via specialized mapping functions for both left and right values
 * @param L The current type of left value
 * @param R The current type of right value
 * @param T The type of output value
 * @param ifLeft A function for converting [L] values to [T]
 * @param ifRight A function for converting [R] values to [T]
 */
@ExperimentalApi
public inline fun <L, R, T> Either<L, R>.fold(ifLeft: (left: L) -> T, ifRight: (right: R) -> T): T = when (this) {
    is Either.Left -> ifLeft(value)
    is Either.Right -> ifRight(value)
}

/**
 * Returns the left value or right value
 * @param T The type of values in left/right
 */
@ExperimentalApi
public fun <T> Either<T, T>.merge(): T = fold({ it }, { it })
