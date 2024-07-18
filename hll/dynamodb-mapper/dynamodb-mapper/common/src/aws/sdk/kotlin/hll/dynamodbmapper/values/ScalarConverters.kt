/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.values

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

public object BooleanConverter : ValueConverter<Boolean> {
    override fun fromAv(attr: AttributeValue): Boolean = attr.asBool()
    override fun toAv(value: Boolean): AttributeValue = AttributeValue.Bool(value)
}

public object IntConverter : ValueConverter<Int> {
    override fun fromAv(attr: AttributeValue): Int = attr.asN().toInt()
    override fun toAv(value: Int): AttributeValue = AttributeValue.N(value.toString())
}

public object StringConverter : ValueConverter<String> {
    override fun fromAv(attr: AttributeValue): String = attr.asS()
    override fun toAv(value: String): AttributeValue = AttributeValue.S(value)
}
