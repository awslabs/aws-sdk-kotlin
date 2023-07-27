/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.test.defaultSettings
import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model
import kotlin.test.assertTrue

/**
 * Verify [UnwrappedXmlOutputIntegration] is enabled for proper service (S3)
 */
class UnwrappedXmlOutputIntegrationTest {
    @Test
    fun test() {
        val model = model("NotS3")
        val actual = UnwrappedXmlOutputIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(actual)
    }

    @Test
    fun test2() {
        val model = model("S3")
        val actual = UnwrappedXmlOutputIntegration().enabledForService(model, model.defaultSettings())
        assertTrue(actual)
    }
}

// Test s3 model where only the service name matters
private fun model(serviceName: String): Model =
    """
        @http(method: "PUT", uri: "/foo")
        operation Foo { }
        
        @http(method: "POST", uri: "/bar")
        operation Bar { }
    """
        .prependNamespaceAndService(operations = listOf("Foo", "Bar"), serviceName = serviceName)
        .toSmithyModel()
