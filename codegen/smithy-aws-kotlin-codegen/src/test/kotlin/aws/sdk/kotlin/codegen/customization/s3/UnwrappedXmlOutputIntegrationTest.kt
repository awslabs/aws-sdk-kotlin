/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.testutil.model
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.test.defaultSettings
import kotlin.test.assertTrue

/**
 * Verify [UnwrappedXmlOutputIntegration] is enabled for proper service (S3)
 */
class UnwrappedXmlOutputIntegrationTest {
    @Test
    fun testNonS3Model() {
        val model = model("NotS3")
        val actual = UnwrappedXmlOutputIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(actual)
    }

    @Test
    fun testS3Model() {
        val model = model("S3")
        val actual = UnwrappedXmlOutputIntegration().enabledForService(model, model.defaultSettings())
        assertTrue(actual)
    }
}
