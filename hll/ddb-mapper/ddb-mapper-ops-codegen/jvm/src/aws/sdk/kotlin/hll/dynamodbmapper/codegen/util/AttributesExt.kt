/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.util

import aws.smithy.kotlin.runtime.collections.AttributeKey
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.merge
import aws.smithy.kotlin.runtime.collections.toMutableAttributes

internal operator fun Attributes.plus(other: Attributes): Attributes = toMutableAttributes().apply { merge(other) }

internal operator fun <T : Any> Attributes.plus(other: Pair<AttributeKey<T>, T?>): Attributes =
    toMutableAttributes().apply {
        other.second?.let { set(other.first, it) } ?: remove(other.first)
    }
