/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.pipeline.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

internal interface Combinable<T, V> {
    operator fun plus(value: V): T
}

internal interface ErrorCombinable<T> {
    val error: Throwable?
    operator fun plus(e: Throwable?): T
}

internal fun Throwable?.suppressing(e: Throwable?) = this?.apply { e?.let(::addSuppressed) } ?: e

@OptIn(ExperimentalContracts::class)
internal fun <T : Any> requireNull(value: T?, lazyMessage: () -> Any): T? {
    contract {
        returns() implies (value == null)
    }

    if (value == null) {
        return null
    } else {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}
