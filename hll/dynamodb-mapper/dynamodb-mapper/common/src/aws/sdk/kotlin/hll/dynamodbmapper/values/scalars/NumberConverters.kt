/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.scalars

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * The base class for numeric [ValueConverter] instances
 * @param N The type of number handled by this converter
 * @param parse A function which parses a [String] into a number of type [N] if possible or `null` if the parsing failed
 */
public abstract class NumberConverter<N>(private val parse: (String) -> N?) : ValueConverter<N> {
    override fun fromAv(attr: AttributeValue): N {
        val n = attr.asN()
        return requireNotNull(parse(attr.asN())) { "Error parsing $n as a number" }
    }

    override fun toAv(value: N): AttributeValue = AttributeValue.N(value.toString())
}

/**
 * Converts between [Byte] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class ByteConverter : NumberConverter<Byte>(String::toByteOrNull) {
    public companion object {
        /**
         * The default instance of [ByteConverter]
         */
        public val Default: ByteConverter = ByteConverter()
    }
}

/**
 * Converts between [Double] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class DoubleConverter : NumberConverter<Double>(String::toDoubleOrNull) {
    override fun toAv(value: Double): AttributeValue {
        require(value.isFinite()) { "Cannot convert $value: only finite numbers are supported" }
        return super.toAv(value)
    }

    public companion object {
        /**
         * The default instance of [DoubleConverter]
         */
        public val Default: DoubleConverter = DoubleConverter()
    }
}

/**
 * Converts between [Float] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class FloatConverter : NumberConverter<Float>(String::toFloatOrNull) {
    override fun toAv(value: Float): AttributeValue {
        require(value.isFinite()) { "Cannot convert $value: only finite numbers are supported" }
        return super.toAv(value)
    }

    public companion object {
        /**
         * The default instance of [FloatConverter]
         */
        public val Default: FloatConverter = FloatConverter()
    }
}

/**
 * Converts between [Int] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class IntConverter : NumberConverter<Int>(String::toIntOrNull) {
    public companion object {
        /**
         * The default instance of [IntConverter]
         */
        public val Default: IntConverter = IntConverter()
    }
}

/**
 * Converts between [Long] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class LongConverter : NumberConverter<Long>(String::toLongOrNull) {
    public companion object {
        /**
         * The default instance of [LongConverter]
         */
        public val Default: LongConverter = LongConverter()
    }
}

/**
 * Converts between [Short] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class ShortConverter : NumberConverter<Short>(String::toShortOrNull) {
    public companion object {
        /**
         * The default instance of [ShortConverter]
         */
        public val Default: ShortConverter = ShortConverter()
    }
}

/**
 * Converts between [UByte] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class UByteConverter : NumberConverter<UByte>(String::toUByteOrNull) {
    public companion object {
        /**
         * The default instance of [UByteConverter]
         */
        public val Default: UByteConverter = UByteConverter()
    }
}

/**
 * Converts between [UInt] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class UIntConverter : NumberConverter<UInt>(String::toUIntOrNull) {
    public companion object {
        /**
         * The default instance of [UIntConverter]
         */
        public val Default: UIntConverter = UIntConverter()
    }
}

/**
 * Converts between [ULong] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class ULongConverter : NumberConverter<ULong>(String::toULongOrNull) {
    public companion object {
        /**
         * The default instance of [ULongConverter]
         */
        public val Default: ULongConverter = ULongConverter()
    }
}

/**
 * Converts between [UShort] and
 * [DynamoDB `N` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.Number)
 */
public class UShortConverter : NumberConverter<UShort>(String::toUShortOrNull) {
    public companion object {
        /**
         * The default instance of [UShortConverter]
         */
        public val Default: UShortConverter = UShortConverter()
    }
}
