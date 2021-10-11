/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.smithy.kotlin.runtime.io.*
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.epochMilliseconds
import aws.smithy.kotlin.runtime.time.fromEpochMilliseconds
import aws.smithy.kotlin.runtime.util.Uuid

internal enum class HeaderType(val value: Byte) {
    TRUE(0),
    FALSE(1),
    BYTE(2),
    INT16(3),
    INT32(4),
    INT64(5),
    BYTE_ARRAY(6),
    STRING(7),
    TIMESTAMP(8),
    UUID(9);

    companion object {
        /**
         * Construct [HeaderType] from raw value
         */
        fun fromTypeId(value: Byte): HeaderType =
            requireNotNull(values().find { it.value == value }) { "Unknown HeaderType: $value" }
    }
}

/**
 * Event stream frame typed header value
 */
@InternalSdkApi
public sealed class HeaderValue {
    public data class Bool(val value: Boolean) : HeaderValue()
    public data class Byte(val value: UByte) : HeaderValue()
    public data class Int16(val value: Short) : HeaderValue()
    public data class Int32(val value: Int) : HeaderValue()
    public data class Int64(val value: Long) : HeaderValue()

    public data class ByteArray(val value: kotlin.ByteArray) : HeaderValue() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ByteArray

            if (!value.contentEquals(other.value)) return false

            return true
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    public data class String(val value: kotlin.String) : HeaderValue()
    public data class Timestamp(val value: Instant) : HeaderValue()
    public data class Uuid(val value: aws.smithy.kotlin.runtime.util.Uuid) : HeaderValue()

    /**
     * Encode a header value to [dest]
     */
    public fun encode(dest: MutableBuffer): Unit = when (this) {
        is Bool -> {
            val type = if (value) HeaderType.TRUE else HeaderType.FALSE
            dest.writeHeader(type)
        }
        is Byte -> {
            dest.writeHeader(HeaderType.BYTE)
            dest.writeByte(value.toByte())
        }
        is Int16 -> {
            dest.writeHeader(HeaderType.INT16)
            dest.writeShort(value)
        }
        is Int32 -> {
            dest.writeHeader(HeaderType.INT32)
            dest.writeInt(value)
        }
        is Int64 -> {
            dest.writeHeader(HeaderType.INT64)
            dest.writeLong(value)
        }
        is ByteArray -> {
            dest.writeHeader(HeaderType.BYTE_ARRAY)
            check(value.size in 0..UShort.MAX_VALUE.toInt()) { "HeaderValue ByteArray too long" }
            dest.writeUShort(value.size.toUShort())
            dest.writeFully(value)
        }
        is String -> {
            val bytes = value.encodeToByteArray()
            check(bytes.size in 0..UShort.MAX_VALUE.toInt()) { "HeaderValue String too long" }
            dest.writeHeader(HeaderType.STRING)
            dest.writeUShort(bytes.size.toUShort())
            dest.writeFully(bytes)
        }
        is Timestamp -> {
            dest.writeHeader(HeaderType.TIMESTAMP)
            dest.writeLong(value.epochMilliseconds)
        }
        is Uuid -> {
            dest.writeHeader(HeaderType.UUID)
            dest.writeLong(value.high)
            dest.writeLong(value.low)
        }
    }

    public companion object {
        public fun decode(buffer: Buffer): HeaderValue {
            val type = buffer.readByte().let { HeaderType.fromTypeId(it) }
            return when (type) {
                HeaderType.TRUE -> HeaderValue.Bool(true)
                HeaderType.FALSE -> HeaderValue.Bool(false)
                HeaderType.BYTE -> HeaderValue.Byte(buffer.readByte().toUByte())
                HeaderType.INT16 -> HeaderValue.Int16(buffer.readShort())
                HeaderType.INT32 -> HeaderValue.Int32(buffer.readInt())
                HeaderType.INT64 -> HeaderValue.Int64(buffer.readLong())
                HeaderType.BYTE_ARRAY, HeaderType.STRING -> {
                    val len = buffer.readUShort()
                    if (buffer.readRemaining < len.toULong()) {
                        throw IllegalStateException("Invalid HeaderValue; type=$type, len=$len; readRemaining: ${buffer.readRemaining}")
                    }
                    val bytes = ByteArray(len.toInt())
                    buffer.readFully(bytes)
                    when (type) {
                        HeaderType.STRING -> HeaderValue.String(bytes.decodeToString())
                        HeaderType.BYTE_ARRAY -> HeaderValue.ByteArray(bytes)
                        else -> throw IllegalStateException("Invalid HeaderValue")
                    }
                }
                HeaderType.TIMESTAMP -> {
                    val epochMilli = buffer.readLong()
                    HeaderValue.Timestamp(Instant.fromEpochMilliseconds(epochMilli))
                }
                HeaderType.UUID -> {
                    val high = buffer.readLong()
                    val low = buffer.readLong()
                    HeaderValue.Uuid(Uuid(high, low))
                }
            }
        }
    }
}

private fun MutableBuffer.writeHeader(headerType: HeaderType) = writeByte(headerType.value)
