/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class SourceSetIntegrationTest {
    
    @Test
    fun `source set integration utility exists`() {
        // Basic test to verify the SourceSetIntegration object exists
        assertTrue(SourceSetIntegration != null)
    }
    
    @Test
    fun `IDE integration can be configured without plugins`() {
        val project = ProjectBuilder.builder().build()
        
        // Create a generation task
        val generateTask = project.tasks.register("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure IDE integration (should work without plugins)
        SourceSetIntegration.configureIdeIntegration(project, generateTask)
        
        // Verify configuration completed without errors
        assertTrue(true) // Basic smoke test
    }
    
    @Test
    fun `incremental build support can be configured`() {
        val project = ProjectBuilder.builder().build()
        
        // Create a generation task
        val generateTask = project.tasks.register("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure incremental build support
        SourceSetIntegration.configureIncrementalBuild(project, generateTask)
        
        // Verify configuration completed without errors
        assertTrue(true) // Basic smoke test
    }
}
