/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests that validate the plugin functionality.
 * These tests focus on plugin integration without requiring full Gradle TestKit execution.
 */
class IntegrationTest {
    
    @Test
    fun `plugin can be applied and configured successfully`() {
        val project = ProjectBuilder.builder().build()
        
        // Apply the plugin
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify plugin was applied
        assertTrue(project.plugins.hasPlugin("aws.sdk.kotlin.custom-sdk-build"))
        
        // Verify extension was registered
        val extension = project.extensions.findByType(CustomSdkBuildExtension::class.java)
        assertTrue(extension != null)
    }
    
    @Test
    fun `plugin configuration works with multiple services`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Configure multiple services
        extension.s3 {
            operations(S3Operation.GetObject, S3Operation.PutObject)
        }
        extension.dynamodb {
            operations(DynamodbOperation.GetItem, DynamodbOperation.PutItem)
        }
        
        // Validate configuration
        extension.validate()
        
        // Verify configuration
        val selectedOperations = extension.getSelectedOperations()
        assertTrue(selectedOperations.size == 2)
        assertTrue(selectedOperations["s3"]?.size == 2)
        assertTrue(selectedOperations["dynamodb"]?.size == 2)
    }
    
    @Test
    fun `plugin supports incremental build configuration`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        extension.s3 {
            operations(S3Operation.GetObject)
        }
        
        // Create and configure generation task
        val generateTask = project.tasks.create("generateCustomSdk", GenerateCustomSdkTask::class.java)
        generateTask.selectedOperations.set(extension.getSelectedOperations())
        
        // Verify cache key generation works
        val cacheKey = generateTask.cacheKeyComponents
        assertTrue(cacheKey.isNotEmpty())
        assertTrue(cacheKey.contains("operations:"))
        assertTrue(cacheKey.contains("s3="))
    }
    
    @Test
    fun `plugin handles configuration changes correctly`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Initial configuration
        extension.s3 {
            operations(S3Operation.GetObject)
        }
        
        val initialOperations = extension.getSelectedOperations()
        assertTrue(initialOperations["s3"]?.size == 1)
        
        // Modify configuration
        extension.s3 {
            operations(S3Operation.GetObject, S3Operation.PutObject)
        }
        
        val modifiedOperations = extension.getSelectedOperations()
        assertTrue(modifiedOperations["s3"]?.size == 2)
    }
    
    @Test
    fun `plugin validates configuration and provides helpful errors`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Try to validate empty configuration
        try {
            extension.validate()
            assertTrue(false, "Expected validation to fail for empty configuration")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("No services configured") == true)
        }
    }
    
    @Test
    fun `plugin integrates with build cache optimization`() {
        val project = ProjectBuilder.builder().build()
        
        // Create a generation task
        val generateTask = project.tasks.register("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure build optimizations
        BuildCacheOptimization.configureBuildCache(project, generateTask)
        BuildCacheOptimization.configurePerformanceMonitoring(project, generateTask)
        BuildCacheOptimization.configureMemoryOptimization(project, generateTask)
        
        // Verify configuration completed without errors
        assertTrue(true)
    }
    
    @Test
    fun `plugin supports build cache with proper annotations`() {
        val project = ProjectBuilder.builder().build()
        
        val task = project.tasks.create("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure task
        task.selectedOperations.set(mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject", "com.amazonaws.s3#PutObject")
        ))
        task.packageName.set("test.custom.sdk")
        task.packageVersion.set("1.0.0")
        
        // Verify task is cacheable
        val cacheableAnnotation = task.javaClass.getAnnotation(org.gradle.api.tasks.CacheableTask::class.java)
        assertTrue(cacheableAnnotation != null)
        
        // Verify cache key generation
        val cacheKey = task.cacheKeyComponents
        assertTrue(cacheKey.contains("operations:"))
        assertTrue(cacheKey.contains("package:test.custom.sdk"))
        assertTrue(cacheKey.contains("version:1.0.0"))
    }
    
    @Test
    fun `plugin provides comprehensive validation feedback`() {
        val project = ProjectBuilder.builder().build()
        
        // Test validation with various error conditions
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject"),
            "invalid-service!" to listOf("invalid-operation"),
            "dynamodb" to listOf("com.amazonaws.s3#GetObject") // Wrong service for operation
        )
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, "invalid.package.Name!", "invalid-version"
        )
        
        // Should have multiple errors
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.code == "INVALID_SERVICE_NAME" })
        assertTrue(result.errors.any { it.code == "INVALID_OPERATION_SHAPE_ID" })
        assertTrue(result.errors.any { it.code == "OPERATION_SERVICE_MISMATCH" })
        assertTrue(result.errors.any { it.code == "INVALID_PACKAGE_NAME" })
        assertTrue(result.errors.any { it.code == "INVALID_PACKAGE_VERSION" })
        
        // Should not be valid
        assertTrue(!result.isValid)
    }
    
    @Test
    fun `plugin handles model file processing correctly`() {
        val tempDir = createTempDir()
        
        try {
            // Create test model files
            File(tempDir, "s3.json").writeText("{\"service\": \"s3\"}")
            File(tempDir, "dynamodb.json").writeText("{\"service\": \"dynamodb\"}")
            File(tempDir, "not-a-model.txt").writeText("ignored")
            
            val modelFiles = BuildCacheOptimization.optimizeModelFileLoading(tempDir)
            
            // Should load only JSON files, sorted alphabetically
            assertTrue(modelFiles.size == 2)
            assertTrue(modelFiles[0].name == "dynamodb.json")
            assertTrue(modelFiles[1].name == "s3.json")
            
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `plugin error handling provides helpful diagnostics`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        
        // Test model loading error handling
        val nonExistentDir = File("/non/existent/directory")
        val cause = java.io.IOException("Directory not found")
        
        try {
            ErrorHandling.handleModelLoadingError(nonExistentDir, cause, logger)
            assertTrue(false, "Expected error handling to throw exception")
        } catch (e: org.gradle.api.GradleException) {
            assertTrue(e.message?.contains("Model loading failed") == true)
        }
        
        // Test error summary creation
        val summary = ErrorHandling.createErrorSummary(RuntimeException("Test error"))
        assertTrue(summary.contains("❌ Custom SDK Build Failed"))
        assertTrue(summary.contains("Test error"))
    }
}
