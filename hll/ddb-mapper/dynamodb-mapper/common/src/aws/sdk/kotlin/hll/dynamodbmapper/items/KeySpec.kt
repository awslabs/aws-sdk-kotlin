/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Companion.Number
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Defines a specification for a single key attribute
 * @param K The type of the key property, either [String], [Number], or [ByteArray]
 */
public sealed interface KeySpec<in K> {
    public companion object {
        /**
         * Creates a new [ByteArray] key specification
         * @param name The name of the key attribute
         */
        public fun ByteArray(name: String): KeySpec<ByteArray> = ByteArrayImpl(name)

        /**
         * Creates a new [Number] key specification
         * @param name The name of the key attribute
         */
        public fun Number(name: String): KeySpec<Number> = NumberImpl(name)

        /**
         * Creates a new [String] key specification
         * @param name The name of the key attribute
         */
        public fun String(name: String): KeySpec<String> = StringImpl(name)
    }

    /**
     * The name of the key attribute
     */
    public val name: String

    /**
     * Given a value for this key attribute, convert into a field
     * @param value The value to use for the key attribute
     */
    public fun toField(value: K): Pair<String, AttributeValue>

    private data class ByteArrayImpl(override val name: String) : KeySpec<ByteArray> {
        override fun toField(value: ByteArray) = name to AttributeValue.B(value)
    }

    private data class NumberImpl(override val name: String) : KeySpec<Number> {
        override fun toField(value: Number) = name to AttributeValue.N(value.toString())
    }

    private data class StringImpl(override val name: String) : KeySpec<String> {
        override fun toField(value: String) = name to AttributeValue.S(value)
    }
}
