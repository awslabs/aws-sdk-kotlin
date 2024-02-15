/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class PackageManifestTest {
    @Test
    fun testValidate() {
        val manifest = PackageManifest(
            listOf(
                PackageMetadata("Package 1", "aws.sdk.kotlin.services.package1", "package1", "AwsSdkKotlinPackage1"),
                PackageMetadata("Package 2", "aws.sdk.kotlin.services.package2", "package2", "AwsSdkKotlinPackage2"),
            ),
        )

        manifest.validate()

        val badManifest = manifest.copy(
            manifest.packages + listOf(
                PackageMetadata("Package 2", "aws.sdk.kotlin.services.package2", "package2", "AwsSdkKotlinPackage2"),
            ),
        )

        val ex = assertFailsWith<IllegalStateException> { badManifest.validate() }

        assertContains(ex.message!!, "multiple packages with same sdkId `Package 2`")
    }
}
