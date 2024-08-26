@file:Suppress("ktlint:standard:no-empty-file")
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.tests.plugins

// FIXME Disabled until local plugin build / publish can be untangled. See build.gradle.kts for more details.
// class UserTest {
//    @Ignore
//    @Test
//    fun testConversion() {
//        val user = User(123, "Steve", "Rogers", 84)
//        val converted = UserConverter.toItem(user)
//
//        assertEquals(
//            itemOf(
//                "id" to AttributeValue.N("123"),
//                "fName" to AttributeValue.S("Steve"),
//                "lName" to AttributeValue.S("Rogers"),
//                "age" to AttributeValue.N("84"),
//            ),
//            converted,
//        )
//
//        val unconverted = UserConverter.fromItem(converted)
//
//        assertEquals(user, unconverted)
//    }
// }
