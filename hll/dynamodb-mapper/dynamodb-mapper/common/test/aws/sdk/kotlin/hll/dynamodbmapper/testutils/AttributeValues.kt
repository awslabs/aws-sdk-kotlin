/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.testutils

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun attr(value: Boolean) = AttributeValue.Bool(value)
fun attr(value: ByteArray) = AttributeValue.B(value)
fun attr(value: List<Any?>) = AttributeValue.L(value.map(::dynamicAttr))
fun attr(vararg value: Any?) = attr(value.toList())
fun attr(value: Map<String, Any?>) = AttributeValue.M(value.mapValues { (_, v) -> dynamicAttr(v) })
fun attr(vararg values: Pair<String, Any?>) = attr(values.toMap())
fun attr(value: Nothing?) = AttributeValue.Null(true)
fun attr(value: Number) = AttributeValue.N(value.toString())
fun attr(value: Set<ByteArray>) = AttributeValue.Bs(value.toList())
fun attr(value: Set<Number>) = AttributeValue.Ns(value.map(Number::toString))
fun attr(value: Set<String>) = AttributeValue.Ss(value.toList())
fun attr(value: String) = AttributeValue.S(value)

// The unsigned types don't implement `Number` and so have to be handled individually

@JvmName("attrUByte")
fun attr(value: Set<UByte>) = AttributeValue.Ns(value.map(UByte::toString))

@JvmName("attrUInt")
fun attr(value: Set<UInt>) = AttributeValue.Ns(value.map(UInt::toString))

@JvmName("attrULong")
fun attr(value: Set<ULong>) = AttributeValue.Ns(value.map(ULong::toString))

@JvmName("attrUShort")
fun attr(value: Set<UShort>) = AttributeValue.Ns(value.map(UShort::toString))

/**
 * Converts a map of strings to values to a map of strings to [AttributeValue]
 * @param item The item to convert
 */
fun ddbItem(item: Map<String, Any?>) = item.mapValues { (_, v) -> dynamicAttr(v) }

/**
 * Converts a collection of tuples of strings to values to a map of strings to [AttributeValue]
 * @param attributes The attributes to convert
 */
fun ddbItem(vararg attributes: Pair<String, Any>) = ddbItem(attributes.toMap())

@Suppress("UNCHECKED_CAST")
fun dynamicAttr(value: Any?): AttributeValue = when (value) {
    null -> attr(null)
    is Boolean -> attr(value)
    is ByteArray -> attr(value)
    is List<*> -> attr(value)
    is Map<*, *> -> attr(value as Map<String, Any?>)
    is Number -> attr(value)
    is Set<*> -> {
        require(value.isNotEmpty()) { "Cannot determine type of empty set at runtime" }
        val types = value.groupBy { it.getClass() }.keys
        val type = types.singleOrNull() ?: error("Mixed set element types: $types")

        when {
            type.isSubclassOf(ByteArray::class) -> attr(value as Set<ByteArray>)
            type.isSubclassOf(Number::class) -> attr(value as Set<Number>)
            type.isSubclassOf(String::class) -> attr(value as Set<String>)
            type.isSubclassOf(UByte::class) -> attr(value as Set<UByte>)
            type.isSubclassOf(UInt::class) -> attr(value as Set<UInt>)
            type.isSubclassOf(ULong::class) -> attr(value as Set<ULong>)
            type.isSubclassOf(UShort::class) -> attr(value as Set<UShort>)
            else -> error("Unsupported set element type $type")
        }
    }
    is String -> attr(value)
    else -> error("Unsupported attribute value type ${value::class}")
}

private fun Any?.getClass(): KClass<*> = if (this == null) Nothing::class else (this::class)
