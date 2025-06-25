/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.security.MessageDigest

/**
 * Utilities for optimizing build cache performance and incremental builds.
 */
object BuildCacheOptimization {
    
    /**
     * Configure build cache settings for the custom SDK generation task.
     */
    fun configureBuildCache(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        project.afterEvaluate {
            generateTask.get().apply {
                // Enable caching for this task
                outputs.cacheIf { true }
                
                // Configure cache key normalization
                configureCacheKeyNormalization(this)
                
                // Set up cache debugging if enabled
                if (project.hasProperty("customSdk.debug.cache")) {
                    configureCacheDebugging(project, this)
                }
            }
        }
        
        project.logger.info("Configured build cache optimization for custom SDK generation")
    }
    
    /**
     * Configure cache key normalization for better cache hit rates.
     */
    private fun configureCacheKeyNormalization(task: GenerateCustomSdkTask) {
        // The task already has proper input annotations
        // Additional normalization can be added here if needed
        
        // Ensure deterministic ordering of operations for consistent cache keys
        task.outputs.doNotCacheIf("No operations selected") { 
            task.selectedOperations.get().isEmpty()
        }
    }
    
    /**
     * Configure cache debugging for troubleshooting cache misses.
     */
    private fun configureCacheDebugging(project: Project, task: GenerateCustomSdkTask) {
        task.doFirst {
            project.logger.lifecycle("Custom SDK Cache Debug:")
            project.logger.lifecycle("  Cache key components: ${task.cacheKeyComponents}")
            project.logger.lifecycle("  Selected operations: ${task.selectedOperations.get()}")
            project.logger.lifecycle("  Package name: ${task.packageName.get()}")
            project.logger.lifecycle("  Package version: ${task.packageVersion.get()}")
        }
    }
    
    /**
     * Optimize model file loading for better performance.
     */
    fun optimizeModelFileLoading(modelsDirectory: File): List<File> {
        if (!modelsDirectory.exists()) {
            return emptyList()
        }
        
        // Load only JSON model files and sort for deterministic processing
        return modelsDirectory.listFiles { file ->
            file.isFile && file.extension == "json"
        }?.sortedBy { it.name } ?: emptyList()
    }
    
    /**
     * Create a stable hash for cache key generation.
     */
    fun createStableHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }.take(16)
    }
    
    /**
     * Check if incremental build is beneficial based on input changes.
     */
    fun shouldUseIncrementalBuild(
        previousOperations: Map<String, List<String>>,
        currentOperations: Map<String, List<String>>
    ): Boolean {
        // Use incremental build if operations haven't changed significantly
        val previousSet = previousOperations.values.flatten().toSet()
        val currentSet = currentOperations.values.flatten().toSet()
        
        val addedOperations = currentSet - previousSet
        val removedOperations = previousSet - currentSet
        
        // Use incremental if changes are minimal (less than 20% change)
        val totalOperations = maxOf(previousSet.size, currentSet.size)
        val changedOperations = addedOperations.size + removedOperations.size
        
        return totalOperations > 0 && (changedOperations.toDouble() / totalOperations) < 0.2
    }
    
    /**
     * Configure performance monitoring for the generation task.
     */
    fun configurePerformanceMonitoring(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        project.afterEvaluate {
            generateTask.get().apply {
                var startTime: Long = 0
                
                doFirst {
                    startTime = System.currentTimeMillis()
                    project.logger.info("Starting custom SDK generation...")
                }
                
                doLast {
                    val duration = System.currentTimeMillis() - startTime
                    val operations = selectedOperations.get().values.flatten().size
                    
                    project.logger.lifecycle("Custom SDK generation completed:")
                    project.logger.lifecycle("  Duration: ${duration}ms")
                    project.logger.lifecycle("  Operations: $operations")
                    if (duration > 0) {
                        project.logger.lifecycle("  Performance: ${operations.toDouble() / (duration / 1000.0)} ops/sec")
                    }
                }
            }
        }
    }
    
    /**
     * Configure memory optimization for large SDK generations.
     */
    fun configureMemoryOptimization(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        project.afterEvaluate {
            generateTask.get().apply {
                doFirst {
                    val operations = selectedOperations.get().values.flatten().size
                    
                    if (operations > 100) {
                        project.logger.info("Large SDK generation detected ($operations operations)")
                        project.logger.info("Consider increasing JVM heap size if generation fails")
                    }
                }
            }
        }
    }
}
