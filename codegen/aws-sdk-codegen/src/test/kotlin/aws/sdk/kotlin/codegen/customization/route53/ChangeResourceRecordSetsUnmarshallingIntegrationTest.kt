/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.route53

import org.junit.jupiter.api.Test
import software.amazon.smithy.kotlin.codegen.test.defaultSettings
import software.amazon.smithy.kotlin.codegen.test.prependNamespaceAndService
import software.amazon.smithy.kotlin.codegen.test.toSmithyModel
import software.amazon.smithy.model.Model
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangeResourceRecordSetsUnmarshallingIntegrationTest {
    @Test
    fun nonRoute53ModelIntegration() {
        val model = sampleModel("not route 53")
        val isEnabledForModel = ChangeResourceRecordSetsUnmarshallingIntegration().enabledForService(model, model.defaultSettings())
        assertFalse(isEnabledForModel)
    }

    @Test
    fun route53ModelIntegration() {
        val model = sampleModel("route 53")
        val isEnabledForModel = ChangeResourceRecordSetsUnmarshallingIntegration().enabledForService(model, model.defaultSettings())
        assertTrue(isEnabledForModel)
    }
}

private fun sampleModel(serviceName: String): Model =
    """
        @http(method: "PUT", uri: "/foo")
        operation Foo { }
        
        @http(method: "POST", uri: "/bar")
        operation Bar { }
    """
        .prependNamespaceAndService(operations = listOf("Foo", "Bar"), serviceName = serviceName)
        .toSmithyModel()
