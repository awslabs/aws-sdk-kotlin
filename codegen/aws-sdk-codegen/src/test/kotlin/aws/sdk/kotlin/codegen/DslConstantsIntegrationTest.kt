/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DslConstantsIntegrationTest {
    
    @Test
    fun `integration is disabled by default`() {
        val integration = DslConstantsIntegration()
        val model = Model.builder()
            .addShape(
                ServiceShape.builder()
                    .id("com.example#TestService")
                    .version("1.0")
                    .build()
            )
            .build()
        
        val config = Node.parse("""
            {
                "package": {
                    "name": "com.example.test",
                    "version": "1.0.0"
                }
            }
        """.trimIndent()).expectObjectNode()
        
        val settings = KotlinSettings.from(model, config)
        
        // Should be disabled by default (no environment variable set)
        assertFalse(integration.enabledForService(model, settings))
    }
    
    @Test
    fun `integration has correct order`() {
        val integration = DslConstantsIntegration()
        
        // Should run last to ensure all other processing is complete
        assertTrue(integration.order == 127.toByte())
    }
}
