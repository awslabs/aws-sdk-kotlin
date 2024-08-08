/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values.collections

import aws.sdk.kotlin.hll.dynamodbmapper.values.ValueConverter
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

/**
 * Converts between a [Set] of [Number] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 * @param N The type of high-level values which will be converted
 */
public abstract class NumberSetConverter<N : Any>(private val fromString: (String) -> N) : ValueConverter<Set<N>> {
    override fun fromAttributeValue(attr: AttributeValue): Set<N> = attr.asNs().map(fromString).toSet()
    override fun toAttributeValue(value: Set<N>): AttributeValue = AttributeValue.Ns(value.map { it.toString() })
}

/**
 * Converts between a [Set] of [Byte] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class ByteSetConverter : NumberSetConverter<Byte>(String::toByte) {
    public companion object {
        /**
         * The default instance of [ByteSetConverter]
         */
        public val Default: ByteSetConverter = ByteSetConverter()
    }
}

/**
 * Converts between a [Set] of [Double] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class DoubleSetConverter : NumberSetConverter<Double>(String::toDouble) {
    public companion object {
        /**
         * The default instance of [DoubleSetConverter]
         */
        public val Default: DoubleSetConverter = DoubleSetConverter()
    }
}

/**
 * Converts between a [Set] of [Float] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class FloatSetConverter : NumberSetConverter<Float>(String::toFloat) {
    public companion object {
        /**
         * The default instance of [FloatSetConverter]
         */
        public val Default: FloatSetConverter = FloatSetConverter()
    }
}

/**
 * Converts between a [Set] of [Int] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class IntSetConverter : NumberSetConverter<Int>(String::toInt) {
    public companion object {
        /**
         * The default instance of [IntSetConverter]
         */
        public val Default: IntSetConverter = IntSetConverter()
    }
}

/**
 * Converts between a [Set] of [Long] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class LongSetConverter : NumberSetConverter<Long>(String::toLong) {
    public companion object {
        /**
         * The default instance of [LongSetConverter]
         */
        public val Default: LongSetConverter = LongSetConverter()
    }
}

/**
 * Converts between a [Set] of [Short] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class ShortSetConverter : NumberSetConverter<Short>(String::toShort) {
    public companion object {
        /**
         * The default instance of [ShortSetConverter]
         */
        public val Default: ShortSetConverter = ShortSetConverter()
    }
}

/**
 * Converts between a [Set] of [UByte] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class UByteSetConverter : NumberSetConverter<UByte>(String::toUByte) {
    public companion object {
        /**
         * The default instance of [UByteSetConverter]
         */
        public val Default: UByteSetConverter = UByteSetConverter()
    }
}

/**
 * Converts between a [Set] of [UInt] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class UIntSetConverter : NumberSetConverter<UInt>(String::toUInt) {
    public companion object {
        /**
         * The default instance of [UIntSetConverter]
         */
        public val Default: UIntSetConverter = UIntSetConverter()
    }
}

/**
 * Converts between a [Set] of [ULong] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class ULongSetConverter : NumberSetConverter<ULong>(String::toULong) {
    public companion object {
        /**
         * The default instance of [ULongSetConverter]
         */
        public val Default: ULongSetConverter = ULongSetConverter()
    }
}

/**
 * Converts between a [Set] of [UShort] elements and
 * [DynamoDB `NS` values](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/HowItWorks.NamingRulesDataTypes.html#HowItWorks.DataTypes.SetTypes)
 */
public class UShortSetConverter : NumberSetConverter<UShort>(String::toUShort) {
    public companion object {
        /**
         * The default instance of [UShortSetConverter]
         */
        public val Default: UShortSetConverter = UShortSetConverter()
    }
}
