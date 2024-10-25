/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.util

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import kotlin.jvm.JvmName

internal val NULL_ATTR = AttributeValue.Null(true)

internal fun attr(value: Boolean?) = value?.let(AttributeValue::Bool) ?: NULL_ATTR
internal fun attr(value: ByteArray?) = value?.let(AttributeValue::B) ?: NULL_ATTR

@JvmName("attrListAny")
internal fun attr(value: List<Any?>?) = attr(value?.map(::dynamicAttr))

internal fun attr(value: List<AttributeValue>?) = value?.let(AttributeValue::L) ?: NULL_ATTR

@JvmName("attrMapStringAny")
internal fun attr(value: Map<String, Any?>?) = attr(value?.mapValues { (_, v) -> dynamicAttr(v) })

internal fun attr(value: Map<String, AttributeValue>?) = value?.let(AttributeValue::M) ?: NULL_ATTR

@Suppress("UNUSED_PARAMETER")
internal fun attr(value: Nothing?) = NULL_ATTR

internal fun attr(value: Number?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR

@JvmName("attrSetByteArray")
internal fun attr(value: Set<ByteArray>?) = value?.let { AttributeValue.Bs(it.toList()) } ?: NULL_ATTR

@JvmName("attrSetNumber")
internal fun attr(value: Set<Number>?) = value?.let { AttributeValue.Ns(it.map(Number::toString)) } ?: NULL_ATTR

@JvmName("attrSetString")
internal fun attr(value: Set<String>?) = value?.let { AttributeValue.Ss(it.toList()) } ?: NULL_ATTR

internal fun attr(value: String?) = value?.let(AttributeValue::S) ?: NULL_ATTR

@JvmName("attrSetUByte")
internal fun attr(value: Set<UByte>?) = value?.let { AttributeValue.Ns(it.map(UByte::toString)) } ?: NULL_ATTR

@JvmName("attrSetUInt")
internal fun attr(value: Set<UInt>?) = value?.let { AttributeValue.Ns(it.map(UInt::toString)) } ?: NULL_ATTR

@JvmName("attrSetULong")
internal fun attr(value: Set<ULong>?) = value?.let { AttributeValue.Ns(it.map(ULong::toString)) } ?: NULL_ATTR

@JvmName("attrSetUShort")
internal fun attr(value: Set<UShort>?) = value?.let { AttributeValue.Ns(it.map(UShort::toString)) } ?: NULL_ATTR

internal fun attr(value: UByte?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR
internal fun attr(value: UInt?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR
internal fun attr(value: ULong?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR
internal fun attr(value: UShort?) = value?.let { AttributeValue.N(it.toString()) } ?: NULL_ATTR

@Suppress("UNCHECKED_CAST")
internal fun dynamicAttr(value: Any?): AttributeValue = when (value) {
    null -> NULL_ATTR
    is AttributeValue -> value
    is Boolean -> attr(value)
    is ByteArray -> attr(value)
    is List<*> -> attr(value)
    is Map<*, *> -> attr(value as Map<String, Any?>)
    is Number -> attr(value)
    is Set<*> -> when (val type = value.firstOrNull()) { // Attempt to determine set type by first element
        null -> attr(value as Set<String>) // FIXME Is this a bad idea for the empty set case?
        is ByteArray -> attr(value as Set<ByteArray>)
        is Number -> attr(value as Set<Number>)
        is String -> attr(value as Set<String>)
        is UByte -> attr(value as Set<UByte>)
        is UInt -> attr(value as Set<UInt>)
        is ULong -> attr(value as Set<ULong>)
        is UShort -> attr(value as Set<UShort>)
        else -> error("Unsupported set element type $type")
    }
    is String -> attr(value)
    else -> error("Unsupported attribute value type ${value::class}")
}
