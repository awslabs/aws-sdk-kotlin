/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Basic smoke tests for the AWS Custom SDK Build plugin
 */
class AwsCustomSdkBuildPluginTest {
    
    @Test
    fun `plugin can be applied to project`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()
        
        // Apply the plugin
        project.pluginManager.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify the plugin was applied successfully
        assertTrue(project.plugins.hasPlugin(AwsCustomSdkBuildPlugin::class.java))
    }
    
    @Test
    fun `plugin creates extension`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()
        
        // Apply the plugin
        project.pluginManager.apply(AwsCustomSdkBuildPlugin::class.java)
        
        // Verify the extension was created
        val extension = project.extensions.findByName(AwsCustomSdkBuildPlugin.EXTENSION_NAME)
        assertNotNull(extension)
        assertTrue(extension is AwsCustomSdkBuildExtension)
    }
    
    @Test
    fun `plugin creates generate task`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()
        
        // Apply the plugin
        project.pluginManager.apply(AwsCustomSdkBuildPlugin::class.java)
        
        // Verify the task was created
        val task = project.tasks.findByName(AwsCustomSdkBuildPlugin.GENERATE_CLIENTS_TASK_NAME)
        assertNotNull(task)
        assertTrue(task is GenerateCustomClientsTask)
    }
    
    @Test
    fun `extension has default values`() {
        // Create a test project
        val project = ProjectBuilder.builder().build()
        
        // Apply the plugin
        project.pluginManager.apply(AwsCustomSdkBuildPlugin::class.java)
        
        // Get the extension
        val extension = project.extensions.getByType(AwsCustomSdkBuildExtension::class.java)
        
        // Verify default values
        assertTrue(extension.region.get() == "us-east-1")
        assertTrue(extension.packageName.get() == "aws.sdk.kotlin.custom")
        assertTrue(extension.outputDirectory.get().contains("build/generated/aws-custom-sdk"))
    }
}
