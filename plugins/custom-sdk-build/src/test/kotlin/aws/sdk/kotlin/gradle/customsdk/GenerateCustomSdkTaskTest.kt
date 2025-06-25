/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GenerateCustomSdkTaskTest {
    
    @Test
    fun `task can be created and configured`() {
        val project = ProjectBuilder.builder().build()
        
        val task = project.tasks.create("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure the task
        task.selectedOperations.set(mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject", "com.amazonaws.s3#PutObject"),
            "dynamodb" to listOf("com.amazonaws.dynamodb#GetItem")
        ))
        task.packageName.set("test.custom.sdk")
        task.packageVersion.set("1.0.0")
        
        // Verify configuration
        assertEquals("test.custom.sdk", task.packageName.get())
        assertEquals("1.0.0", task.packageVersion.get())
        assertEquals(2, task.selectedOperations.get().size)
    }
    
    @Test
    fun `task has correct default values`() {
        val project = ProjectBuilder.builder().build()
        
        val task = project.tasks.create("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Verify defaults
        assertEquals("aws.sdk.kotlin.services.custom", task.packageName.get())
        assertEquals(project.version.toString(), task.packageVersion.get())
        assertNotNull(task.outputDirectory.get())
    }
    
    @Test
    fun `task can generate placeholder files`() {
        val project = ProjectBuilder.builder().build()
        
        val task = project.tasks.create("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure with minimal setup
        task.selectedOperations.set(mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject")
        ))
        
        // Create models directory
        val modelsDir = File(project.buildDir, "models")
        modelsDir.mkdirs()
        task.modelsDirectory.set(modelsDir)
        
        // Create a simple model file
        val modelFile = File(modelsDir, "s3.json")
        modelFile.writeText("{}")
        
        // Execute the task
        task.generate()
        
        // Verify output was created
        val outputDir = task.outputDirectory.get().asFile
        assertTrue(outputDir.exists())
        
        // Verify smithy-build.json was created
        val projectionFile = File(outputDir, "smithy-build.json")
        assertTrue(projectionFile.exists())
        
        // Verify placeholder client was created
        val clientFile = File(outputDir, "src/main/kotlin/aws/sdk/kotlin/services/custom/CustomSdkClient.kt")
        assertTrue(clientFile.exists())
    }
}
