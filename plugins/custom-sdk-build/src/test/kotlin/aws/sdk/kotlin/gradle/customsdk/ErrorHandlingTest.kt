/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ErrorHandlingTest {
    
    @Test
    fun `model loading error handling works for missing directory`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        val nonExistentDir = File("/non/existent/directory")
        val cause = IOException("Directory not found")
        
        val exception = assertFailsWith<GradleException> {
            ErrorHandling.handleModelLoadingError(nonExistentDir, cause, logger)
        }
        
        assertTrue(exception.message?.contains("Model loading failed") == true)
    }
    
    @Test
    fun `model loading error handling works for empty directory`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        val tempDir = createTempDir()
        
        try {
            val cause = IOException("No models found")
            
            val exception = assertFailsWith<GradleException> {
                ErrorHandling.handleModelLoadingError(tempDir, cause, logger)
            }
            
            assertTrue(exception.message?.contains("Model loading failed") == true)
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `smithy build error handling works`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        val tempFile = createTempFile("projection", ".json")
        
        try {
            tempFile.writeText("{}")
            val cause = IOException("Smithy build failed")
            
            val exception = assertFailsWith<GradleException> {
                ErrorHandling.handleSmithyBuildError(tempFile, cause, logger)
            }
            
            assertTrue(exception.message?.contains("Smithy build failed") == true)
        } finally {
            tempFile.delete()
        }
    }
    
    @Test
    fun `code generation error handling works`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        val tempDir = createTempDir()
        
        try {
            val cause = IOException("Code generation failed")
            
            val exception = assertFailsWith<GradleException> {
                ErrorHandling.handleCodeGenerationError(tempDir, cause, logger)
            }
            
            assertTrue(exception.message?.contains("Code generation failed") == true)
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `dependency resolution error handling works`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        val cause = RuntimeException("Could not resolve dependency")
        
        val exception = assertFailsWith<GradleException> {
            ErrorHandling.handleDependencyResolutionError(cause, logger)
        }
        
        assertTrue(exception.message?.contains("Dependency resolution failed") == true)
    }
    
    @Test
    fun `task execution error handling works`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        val taskName = "generateCustomSdk"
        val cause = IllegalStateException("Task failed")
        
        val exception = assertFailsWith<GradleException> {
            ErrorHandling.handleTaskExecutionError(taskName, cause, logger)
        }
        
        assertTrue(exception.message?.contains("Task execution failed") == true)
    }
    
    @Test
    fun `recovery suggestions work for different error types`() {
        val project = ProjectBuilder.builder().build()
        val logger = project.logger
        
        // Test OutOfMemoryError suggestions
        val memoryError = RuntimeException("OutOfMemoryError occurred")
        ErrorHandling.suggestRecoveryActions(memoryError, logger)
        
        // Test PermissionDenied suggestions
        val permissionError = RuntimeException("PermissionDenied")
        ErrorHandling.suggestRecoveryActions(permissionError, logger)
        
        // Test NoSuchFile suggestions
        val fileError = RuntimeException("NoSuchFile")
        ErrorHandling.suggestRecoveryActions(fileError, logger)
        
        // Test timeout suggestions
        val timeoutError = RuntimeException("timeout occurred")
        ErrorHandling.suggestRecoveryActions(timeoutError, logger)
        
        // Test generic error suggestions
        val genericError = RuntimeException("Generic error")
        ErrorHandling.suggestRecoveryActions(genericError, logger)
        
        // All should complete without throwing exceptions
        assertTrue(true)
    }
    
    @Test
    fun `error summary creation works`() {
        val error = RuntimeException("Test error message")
        
        val summary = ErrorHandling.createErrorSummary(error)
        
        assertTrue(summary.contains("❌ Custom SDK Build Failed"))
        assertTrue(summary.contains("Test error message"))
        assertTrue(summary.contains("For additional help:"))
    }
    
    @Test
    fun `error summary works with null message`() {
        val error = RuntimeException()
        
        val summary = ErrorHandling.createErrorSummary(error)
        
        assertTrue(summary.contains("❌ Custom SDK Build Failed"))
        assertTrue(summary.contains("RuntimeException"))
        assertTrue(summary.contains("For additional help:"))
    }
}
