/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.testutil

import kotlin.test.assertNotEquals

public fun String.lines(fromLine: String, toLine: String): String {
    val allLines = lines()

    val fromIdx = allLines.indexOf(fromLine)
    assertNotEquals(-1, fromIdx, """Could not find from line "$fromLine" in all lines""")

    val toIdxOffset = allLines.drop(fromIdx + 1).indexOf(toLine)
    assertNotEquals(-1, toIdxOffset, """Could not find to line "$toLine" in all lines""")

    val toIdx = toIdxOffset + fromIdx + 1
    return allLines.subList(fromIdx, toIdx + 1).joinToString("\n")
}
