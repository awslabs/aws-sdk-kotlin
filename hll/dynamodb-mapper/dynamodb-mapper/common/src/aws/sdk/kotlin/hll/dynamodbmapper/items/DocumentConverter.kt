/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.items

import aws.sdk.kotlin.hll.dynamodbmapper.model.Item
import aws.sdk.kotlin.hll.dynamodbmapper.model.toItem
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.util.toNumber

public object DocumentConverter : ItemConverter<Document> {
    override fun fromItem(item: Item): Document = item
        .mapValues { (_, attr) -> fromAv(attr) }
        .let(Document::Map)

    override fun toItem(obj: Document, onlyAttributes: Set<String>?): Item {
        require(obj is Document.Map)

        val map = if (onlyAttributes == null) {
            obj
        } else {
            obj.filterKeys { it in onlyAttributes }
        }

        return map.mapValues { (_, value) -> toAv(value) }.toItem()
    }
}

@OptIn(InternalApi::class)
private fun fromAv(attr: AttributeValue): Document? = when (attr) {
    is AttributeValue.Null -> null
    is AttributeValue.N -> Document.Number(attr.value.toNumber()!!) // FIXME need better toNumber logic
    is AttributeValue.S -> Document.String(attr.value)
    is AttributeValue.Bool -> Document.Boolean(attr.value)
    is AttributeValue.L -> Document.List(attr.value.map(::fromAv))
    is AttributeValue.M -> Document.Map(attr.value.mapValues { (_, nestedValue) -> fromAv(nestedValue) })
    else -> error("Documents do not support ${attr::class.qualifiedName}")
}

private fun toAv(value: Document?): AttributeValue = when (value) {
    null -> AttributeValue.Null(true)
    is Document.Number -> AttributeValue.N(value.value.toString())
    is Document.String -> AttributeValue.S(value.value)
    is Document.Boolean -> AttributeValue.Bool(value.value)
    is Document.List -> AttributeValue.L(value.value.map(::toAv))
    is Document.Map -> AttributeValue.M(value.mapValues { (_, nestedValue) -> toAv(nestedValue) })
}
