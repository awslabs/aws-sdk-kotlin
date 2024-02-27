/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.config.sigv4a

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsValidListFormatTest {
    @Test
    fun regexWorks() {
        assertTrue(isValidListFormat("apple"))
        assertTrue(isValidListFormat("apple,banana,orange"))
        assertTrue(isValidListFormat("apple, banana, orange"))
        assertTrue(isValidListFormat("apple, banana, orange     "))

        assertFalse(isValidListFormat(""))
        assertFalse(isValidListFormat("         "))
        assertFalse(isValidListFormat("apple, , orange"))
        assertFalse(isValidListFormat("apple banana"))
        assertFalse(isValidListFormat("apple,banana,orange,"))
    }
}
