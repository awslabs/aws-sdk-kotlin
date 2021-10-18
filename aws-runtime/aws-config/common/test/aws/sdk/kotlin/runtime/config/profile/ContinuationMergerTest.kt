/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ContinuationMergerTest {
    @Test
    fun canMergeContinuations() {
        val positiveTestCases = listOf(
            "# Comment\n[profile foo] ; Comment\nname = value # Comment with ; sign" to "[[profile foo] ; Comment, name = value # Comment with ; sign]",
            "[profile foo]\nname = value\n -continued" to "[[profile foo], name = value\n-continued]",
            "[profile foo]\nname = value\n -continued\n -and-continued" to "[[profile foo], name = value\n-continued\n-and-continued]",
            "[profile foo]\nname = value\n \t -continued \t " to "[[profile foo], name = value\n-continued]",
            "[profile foo]\nname = value\n -continued # Comment" to "[[profile foo], name = value\n-continued # Comment]",
            "[profile foo]\nname = value\n -continued ; Comment" to "[[profile foo], name = value\n-continued ; Comment]",
            "\t \n[profile foo]\n\t\n \nname = value\n\t \n[profile bar]\n \t" to "[[profile foo], name = value, [profile bar]]",
            "[profile foo]\ns3 =\n name = value" to "[[profile foo], s3 =\nname = value]",
        )

        positiveTestCases.forEach { testCase ->
            val actual = mergeContinuations(testCase.first).map { it.content }
            assertEquals(testCase.second, actual.toString())
        }

        val negativeTestCases = listOf(
            " -continued" to "Expected a profile definition",
            "[profile foo]\n -continued" to "Expected a property definition",
            "[profile foo]\nname = value\n[profile foo]\n -continued" to "Expected a property definition"
        )

        negativeTestCases.forEach { testCase ->
            assertFails(testCase.second) { mergeContinuations(testCase.first) }
        }
    }
}
