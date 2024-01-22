/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import kotlin.test.*

class MembershipTest {
    @Test
    fun testIsMember() {
        val unit = Membership(setOf("i1", "i2"), setOf("e1"))
        assertTrue(unit.isMember("i1"))
        assertTrue(unit.isMember("i2"))
        assertFalse(unit.isMember("e1"))

        // test implicit include
        assertTrue(Membership().isMember("i3"))
    }

    @Test
    fun testExcludePrecedence() {
        val unit = Membership(setOf("m1"), setOf("m1"))
        assertFalse(unit.isMember("m1"))
    }

    @Test
    fun testParse() {
        val expected = Membership(
            setOf("i1", "i2"),
            setOf("e1"),
        )

        val actual = parseMembership("+i1,-e1,i2")
        assertEquals(expected, actual)
    }

    @Test
    fun testParseWithConflict() {
        assertFails {
            parseMembership("+i1,-i1")
        }
    }
}
