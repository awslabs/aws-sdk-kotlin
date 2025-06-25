/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomSdkBuildPluginTest {
    
    @Test
    fun `plugin can be applied to project`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify plugin was applied successfully
        assertNotNull(project.plugins.findPlugin(CustomSdkBuildPlugin::class.java))
    }
    
    @Test
    fun `plugin registers extension`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify extension was registered
        val extension = project.extensions.findByType(CustomSdkBuildExtension::class.java)
        assertNotNull(extension)
    }
    
    @Test
    fun `extension can be configured`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Configure the extension
        extension.s3 {
            operations(S3Operation.GetObject, S3Operation.PutObject)
        }
        
        // Verify configuration
        val selectedOperations = extension.getSelectedOperations()
        assertNotNull(selectedOperations["s3"])
    }
    
    @Test
    fun `plugin can be applied without Kotlin plugins`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Configure the extension
        extension.dynamodb {
            operations(DynamodbOperation.GetItem)
        }
        
        // Verify plugin applied successfully
        assertNotNull(project.plugins.findPlugin(CustomSdkBuildPlugin::class.java))
    }
    
    @Test
    fun `plugin configuration works with multiple services`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Configure multiple services
        extension.s3 {
            operations(S3Operation.GetObject)
        }
        extension.lambda {
            operations(LambdaOperation.Invoke)
        }
        
        // Verify configuration
        val selectedOperations = extension.getSelectedOperations()
        assertTrue(selectedOperations.containsKey("s3"))
        assertTrue(selectedOperations.containsKey("lambda"))
    }
}
