/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.benchmarks.service.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentListBuilderTest {
    @Test
    fun testEmpty() {
        val builder = ConcurrentListBuilder<String>()
        assertEquals(listOf(), builder.toList())
    }

    @Test
    fun testNonEmpty() {
        val builder = ConcurrentListBuilder<String>()
        builder.add("a")
        builder.add("b")
        builder.add("c")
        assertEquals(listOf("a", "b", "c"), builder.toList())

        builder.add("d")
        builder.add("e")
        builder.add("f")
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), builder.toList())
    }
}
