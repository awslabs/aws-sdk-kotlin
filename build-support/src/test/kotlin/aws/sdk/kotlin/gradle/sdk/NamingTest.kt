/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import kotlin.test.Test
import kotlin.test.assertEquals

class NamingTest {
    @Test
    fun testPackageNameForService() {
        assertEquals("foobar", packageNameForService("Foo Bar"))
        assertEquals("foobar", packageNameForService("Foo    Bar"))
        assertEquals("foobar", packageNameForService("FoO-BaR"))
    }

    @Test
    fun testKotlinNamespace() {
        assertEquals("foobar", "foo b-ar".kotlinNamespace())
        assertEquals("foobar", " foo   bar  ".kotlinNamespace())
        assertEquals("foo.bar", "foo.b-ar".kotlinNamespace())
    }

    @Test
    fun testSdkIdToArtifactName() {
        assertEquals("foobar", sdkIdToArtifactName("foo bar"))
        assertEquals("foobar", sdkIdToArtifactName("foo-bar"))
        assertEquals("foobar", sdkIdToArtifactName("fOo -Bar"))
    }

    @Test
    fun testSdkIdToModelFilename() {
        assertEquals("foo", sdkIdToModelFilename("Foo"))
        assertEquals("foo-bar", sdkIdToModelFilename("Foo Bar"))
        assertEquals("foo-bar", sdkIdToModelFilename("   Foo     Bar   "))
    }
}
