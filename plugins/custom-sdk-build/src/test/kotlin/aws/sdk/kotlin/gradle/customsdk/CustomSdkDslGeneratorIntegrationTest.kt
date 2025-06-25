/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import kotlin.test.Test
import kotlin.test.assertNotNull

class CustomSdkDslGeneratorIntegrationTest {
    
    @Test
    fun `integration can be instantiated`() {
        val integration = CustomSdkDslGeneratorIntegration()
        assertNotNull(integration)
    }
}
