/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.telemetry

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

class ConcurrentListBuilder<T> {
    private val head = atomic<Node<T>?>(null)

    fun add(value: T) {
        head.update { Node(value, it) }
    }

    fun toList(): List<T> {
        var ptr: Node<T>? = head.value
        return buildList {
            while (ptr != null) {
                add(ptr!!.value)
                ptr = ptr!!.next
            }
        }.reversed()
    }

    private data class Node<T>(val value: T, val next: Node<T>?)
}
