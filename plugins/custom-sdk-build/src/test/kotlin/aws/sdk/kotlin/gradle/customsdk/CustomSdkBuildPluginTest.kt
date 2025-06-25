/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class CustomSdkBuildPluginTest {
    
    @Test
    fun `plugin can be applied to project`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify plugin was applied successfully
        assertNotNull(project.plugins.findPlugin(CustomSdkBuildPlugin::class.java))
    }
}
