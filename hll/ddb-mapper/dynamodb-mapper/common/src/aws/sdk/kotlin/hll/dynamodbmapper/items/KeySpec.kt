/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Defines a specification for a single key attribute
 * @param K The type of the key property, either [String], [Number], or [ByteArray]
 */
public sealed interface KeySpec<in K> {
    public companion object {
        public fun ByteArray(name: String): KeySpec<ByteArray> = ByteArrayImpl(name)

        public fun Number(name: String): KeySpec<Number> = NumberImpl(name)

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
}

private class ByteArrayImpl(override val name: String) : KeySpec<ByteArray> {
    override fun toField(value: ByteArray): Pair<String, AttributeValue> = name to AttributeValue.B(value)
}

private class NumberImpl(override val name: String) : KeySpec<Number> {
    override fun toField(value: Number): Pair<String, AttributeValue> = name to AttributeValue.N(value.toString())
}

private class StringImpl(override val name: String) : KeySpec<String> {
    override fun toField(value: String): Pair<String, AttributeValue> = name to AttributeValue.S(value)
}
