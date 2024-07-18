/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.util

import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.merge
import aws.smithy.kotlin.runtime.collections.toMutableAttributes

/**
 * Combines this [Attributes] collection with another collection and returns the new result
 * @param other The other attributes to merge
 */
operator fun Attributes.plus(other: Attributes): Attributes = toMutableAttributes().apply { merge(other) }

/**
 * Adds another attribute to this collection and returns the new result
 * @param other A tuple of [AttributeKey] to a value (which may be `null`)
 */
operator fun <T : Any> Attributes.plus(other: Pair<AttributeKey<T>, T?>): Attributes =
    toMutableAttributes().apply {
        other.second?.let { set(other.first, it) } ?: remove(other.first)
    }
