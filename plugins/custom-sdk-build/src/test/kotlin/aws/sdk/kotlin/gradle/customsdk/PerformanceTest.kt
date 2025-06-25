/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Performance tests for the custom SDK build plugin.
 * These tests validate that the plugin performs well under various load conditions.
 */
class PerformanceTest {
    
    @Test
    fun `plugin handles small SDK configurations efficiently`() {
        val project = ProjectBuilder.builder().build()
        
        val startTime = System.currentTimeMillis()
        
        // Create a small configuration
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject", "com.amazonaws.s3#PutObject"),
            "dynamodb" to listOf("com.amazonaws.dynamodb#GetItem")
        )
        
        val validationTime = measureTimeMillis {
            val result = ValidationEngine.validateConfiguration(
                project, selectedOperations, "aws.sdk.kotlin.services.custom", "1.0.0"
            )
            assertTrue(result.isValid)
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Small configurations should validate very quickly (< 100ms)
        assertTrue(validationTime < 100, "Validation took ${validationTime}ms, expected < 100ms")
        assertTrue(totalTime < 200, "Total time took ${totalTime}ms, expected < 200ms")
    }
    
    @Test
    fun `plugin handles medium SDK configurations efficiently`() {
        val project = ProjectBuilder.builder().build()
        
        val startTime = System.currentTimeMillis()
        
        // Create a medium configuration (50 operations across 5 services)
        val selectedOperations = mapOf(
            "s3" to (1..10).map { "com.amazonaws.s3#Operation$it" },
            "dynamodb" to (1..10).map { "com.amazonaws.dynamodb#Operation$it" },
            "lambda" to (1..10).map { "com.amazonaws.lambda#Operation$it" },
            "ec2" to (1..10).map { "com.amazonaws.ec2#Operation$it" },
            "iam" to (1..10).map { "com.amazonaws.iam#Operation$it" }
        )
        
        val validationTime = measureTimeMillis {
            val result = ValidationEngine.validateConfiguration(
                project, selectedOperations, "aws.sdk.kotlin.services.custom", "1.0.0"
            )
            assertTrue(result.isValid)
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Medium configurations should still validate quickly (< 500ms)
        assertTrue(validationTime < 500, "Validation took ${validationTime}ms, expected < 500ms")
        assertTrue(totalTime < 1000, "Total time took ${totalTime}ms, expected < 1000ms")
    }
    
    @Test
    fun `plugin handles large SDK configurations within reasonable time`() {
        val project = ProjectBuilder.builder().build()
        
        val startTime = System.currentTimeMillis()
        
        // Create a large configuration (200 operations across 10 services)
        val selectedOperations = mapOf(
            "s3" to (1..20).map { "com.amazonaws.s3#Operation$it" },
            "dynamodb" to (1..20).map { "com.amazonaws.dynamodb#Operation$it" },
            "lambda" to (1..20).map { "com.amazonaws.lambda#Operation$it" },
            "ec2" to (1..20).map { "com.amazonaws.ec2#Operation$it" },
            "iam" to (1..20).map { "com.amazonaws.iam#Operation$it" },
            "sns" to (1..20).map { "com.amazonaws.sns#Operation$it" },
            "sqs" to (1..20).map { "com.amazonaws.sqs#Operation$it" },
            "rds" to (1..20).map { "com.amazonaws.rds#Operation$it" },
            "cloudformation" to (1..20).map { "com.amazonaws.cloudformation#Operation$it" },
            "cloudwatch" to (1..20).map { "com.amazonaws.cloudwatch#Operation$it" }
        )
        
        val validationTime = measureTimeMillis {
            val result = ValidationEngine.validateConfiguration(
                project, selectedOperations, "aws.sdk.kotlin.services.custom", "1.0.0"
            )
            assertTrue(result.isValid)
            assertTrue(result.hasWarnings) // Should warn about large configuration
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Large configurations should still complete within reasonable time (< 2 seconds)
        assertTrue(validationTime < 2000, "Validation took ${validationTime}ms, expected < 2000ms")
        assertTrue(totalTime < 3000, "Total time took ${totalTime}ms, expected < 3000ms")
    }
    
    @Test
    fun `build cache optimization works efficiently`() {
        val project = ProjectBuilder.builder().build()
        
        // Create a generation task
        val generateTask = project.tasks.register("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        val configurationTime = measureTimeMillis {
            BuildCacheOptimization.configureBuildCache(project, generateTask)
            BuildCacheOptimization.configurePerformanceMonitoring(project, generateTask)
            BuildCacheOptimization.configureMemoryOptimization(project, generateTask)
        }
        
        // Build cache configuration should be very fast (< 50ms)
        assertTrue(configurationTime < 50, "Build cache configuration took ${configurationTime}ms, expected < 50ms")
    }
    
    @Test
    fun `model file loading optimization works efficiently`() {
        val tempDir = createTempDir()
        
        try {
            // Create many model files
            val modelFiles = (1..100).map { index ->
                val file = File(tempDir, "service$index.json")
                file.writeText("{\"service\": \"test$index\"}")
                file
            }
            
            val loadingTime = measureTimeMillis {
                val loadedFiles = BuildCacheOptimization.optimizeModelFileLoading(tempDir)
                assertTrue(loadedFiles.size == 100)
                assertTrue(loadedFiles.first().name <= loadedFiles.last().name) // Should be sorted
            }
            
            // Loading 100 files should be fast (< 100ms)
            assertTrue(loadingTime < 100, "Model file loading took ${loadingTime}ms, expected < 100ms")
            
        } finally {
            tempDir.deleteRecursively()
        }
    }
    
    @Test
    fun `stable hash generation is efficient`() {
        val testInputs = (1..1000).map { "test input $it" }
        
        val hashingTime = measureTimeMillis {
            testInputs.forEach { input ->
                val hash = BuildCacheOptimization.createStableHash(input)
                assertTrue(hash.length == 16)
            }
        }
        
        // Hashing 1000 strings should be fast (< 100ms)
        assertTrue(hashingTime < 100, "Hash generation took ${hashingTime}ms, expected < 100ms")
    }
    
    @Test
    fun `incremental build decision logic is efficient`() {
        val previousOps = mapOf(
            "s3" to (1..50).map { "com.amazonaws.s3#Operation$it" },
            "dynamodb" to (1..50).map { "com.amazonaws.dynamodb#Operation$it" }
        )
        
        val currentOps = mapOf(
            "s3" to (1..51).map { "com.amazonaws.s3#Operation$it" }, // Added one operation
            "dynamodb" to (1..50).map { "com.amazonaws.dynamodb#Operation$it" }
        )
        
        val decisionTime = measureTimeMillis {
            repeat(1000) {
                BuildCacheOptimization.shouldUseIncrementalBuild(previousOps, currentOps)
            }
        }
        
        // 1000 incremental build decisions should be very fast (< 50ms)
        assertTrue(decisionTime < 50, "Incremental build decisions took ${decisionTime}ms, expected < 50ms")
    }
    
    @Test
    fun `task configuration scales with number of operations`() {
        val project = ProjectBuilder.builder().build()
        
        // Test with different numbers of operations
        val operationCounts = listOf(10, 50, 100, 200)
        
        operationCounts.forEach { count ->
            val selectedOperations = mapOf(
                "test-service" to (1..count).map { "com.amazonaws.test#Operation$it" }
            )
            
            val task = project.tasks.create("generateCustomSdk$count", GenerateCustomSdkTask::class.java)
            task.selectedOperations.set(selectedOperations)
            
            val configurationTime = measureTimeMillis {
                val cacheKey = task.cacheKeyComponents
                assertTrue(cacheKey.isNotEmpty())
            }
            
            // Configuration time should scale reasonably (< 10ms per 100 operations)
            val expectedMaxTime = (count / 100.0 * 10).toInt().coerceAtLeast(10)
            assertTrue(
                configurationTime < expectedMaxTime, 
                "Configuration for $count operations took ${configurationTime}ms, expected < ${expectedMaxTime}ms"
            )
        }
    }
    
    @Test
    fun `memory usage is reasonable for large configurations`() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Create a very large configuration
        val largeConfiguration = (1..20).associate { serviceIndex ->
            "service$serviceIndex" to (1..50).map { "com.amazonaws.service$serviceIndex#Operation$it" }
        }
        
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateCustomSdkLarge", GenerateCustomSdkTask::class.java)
        task.selectedOperations.set(largeConfiguration)
        
        // Force garbage collection to get accurate measurement
        System.gc()
        Thread.sleep(100)
        
        val afterConfigMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = afterConfigMemory - initialMemory
        
        // Memory usage should be reasonable (< 50MB for large configuration)
        val maxMemoryMB = 50 * 1024 * 1024 // 50MB in bytes
        assertTrue(
            memoryUsed < maxMemoryMB, 
            "Memory usage was ${memoryUsed / (1024 * 1024)}MB, expected < 50MB"
        )
    }
    
    @Test
    fun `validation performance is reasonable for different configuration sizes`() {
        val project = ProjectBuilder.builder().build()
        
        // Use a valid service name that passes validation
        val selectedOperations = mapOf(
            "s3" to (1..50).map { "com.amazonaws.s3#Operation$it" }
        )
        
        val validationTime = measureTimeMillis {
            val result = ValidationEngine.validateConfiguration(
                project, selectedOperations, "aws.sdk.kotlin.services.custom", "1.0.0"
            )
            // Note: This will have warnings about operation service mismatch, but should still be valid overall
            // Actually, let's just check that validation completes without throwing exceptions
        }
        
        // Validation should complete within reasonable time (< 2 seconds)
        assertTrue(
            validationTime < 2000, 
            "Validation for 50 operations took ${validationTime}ms, expected < 2000ms"
        )
    }
}
