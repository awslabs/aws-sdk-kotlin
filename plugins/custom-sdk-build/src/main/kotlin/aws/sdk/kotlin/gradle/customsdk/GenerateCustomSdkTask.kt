/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import java.io.IOException

/**
 * Task that generates custom SDK code using Smithy projections and transforms.
 * 
 * This task takes the user's operation selections and generates a filtered SDK
 * containing only the selected operations and their dependencies.
 * 
 * The task is optimized for build cache and incremental builds:
 * - Uses proper input/output annotations for caching
 * - Supports incremental execution based on input changes
 * - Optimizes model file loading and processing
 */
@CacheableTask
abstract class GenerateCustomSdkTask : DefaultTask() {
    
    /**
     * Map of service names to selected operation shape IDs.
     * This comes from the user's DSL configuration.
     */
    @get:Input
    abstract val selectedOperations: MapProperty<String, List<String>>
    
    /**
     * Package name for the generated SDK.
     */
    @get:Input
    abstract val packageName: Property<String>
    
    /**
     * Package version for the generated SDK.
     */
    @get:Input
    abstract val packageVersion: Property<String>
    
    /**
     * Output directory where the generated SDK code will be written.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    /**
     * Directory containing AWS service model files.
     * Uses RELATIVE path sensitivity for better cache performance.
     */
    @get:Incremental
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modelsDirectory: DirectoryProperty
    
    /**
     * Build cache key components for better cache hit rates.
     * This includes a hash of the selected operations for cache key generation.
     */
    @get:Input
    val cacheKeyComponents: String
        get() = buildString {
            append("operations:")
            selectedOperations.get().entries.sortedBy { it.key }.forEach { (service, operations) ->
                append("$service=${operations.sorted().joinToString(",")};")
            }
            append("package:${packageName.get()}")
            append("version:${packageVersion.get()}")
        }
    
    init {
        description = "Generates custom AWS SDK code with only selected operations"
        group = "aws-sdk-kotlin"
        
        // Set default values
        packageName.convention("aws.sdk.kotlin.services.custom")
        packageVersion.convention(project.version.toString())
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/customSdk"))
        
        // Configure for optimal caching
        outputs.cacheIf { true }
        
        // Configure up-to-date checking
        outputs.upToDateWhen { task ->
            // Task is up-to-date if inputs haven't changed and outputs exist
            val outputDir = outputDirectory.get().asFile
            outputDir.exists() && outputDir.listFiles()?.isNotEmpty() == true
        }
    }
    
    @TaskAction
    fun generate(inputChanges: InputChanges) {
        try {
            val operations = selectedOperations.get()
            val allOperationShapeIds = operations.values.flatten()
            
            logger.info("Generating custom SDK with ${allOperationShapeIds.size} operations across ${operations.size} services")
            logger.info("Incremental: ${inputChanges.isIncremental}")
            
            if (allOperationShapeIds.isEmpty()) {
                throw IllegalStateException("No operations selected for custom SDK generation")
            }
            
            // Log input changes for debugging
            if (inputChanges.isIncremental) {
                inputChanges.getFileChanges(modelsDirectory).forEach { change ->
                    logger.info("Model file ${change.changeType}: ${change.file.name}")
                }
            }
            
            // Clean output directory if not incremental or if configuration changed
            val outputDir = outputDirectory.get().asFile
            if (!inputChanges.isIncremental || hasConfigurationChanged()) {
                logger.info("Performing full regeneration")
                if (outputDir.exists()) {
                    outputDir.deleteRecursively()
                }
            }
            outputDir.mkdirs()
            
            // Create Smithy projection configuration
            val projectionConfig = createSmithyProjection(allOperationShapeIds)
            
            // Write projection configuration to file
            val projectionFile = File(outputDir, "smithy-build.json")
            projectionFile.writeText(projectionConfig)
            
            logger.info("Created Smithy projection configuration at: ${projectionFile.absolutePath}")
            
            // Execute smithy-build to generate the custom SDK
            executeSmithyBuild(projectionFile, outputDir, inputChanges.isIncremental)
            
            // Write cache metadata for next run
            writeCacheMetadata(outputDir)
            
            logger.info("Custom SDK generation completed successfully")
            
        } catch (e: Exception) {
            handleGenerationError(e)
        }
    }
    
    /**
     * Check if configuration has changed since last run.
     */
    private fun hasConfigurationChanged(): Boolean {
        val outputDir = outputDirectory.get().asFile
        val metadataFile = File(outputDir, ".cache-metadata")
        
        if (!metadataFile.exists()) {
            return true
        }
        
        val previousCacheKey = metadataFile.readText().trim()
        val currentCacheKey = cacheKeyComponents
        
        return previousCacheKey != currentCacheKey
    }
    
    /**
     * Write cache metadata for incremental build support.
     */
    private fun writeCacheMetadata(outputDir: File) {
        val metadataFile = File(outputDir, ".cache-metadata")
        metadataFile.writeText(cacheKeyComponents)
    }
    
    /**
     * Create a Smithy projection configuration with the IncludeOperations transform.
     */
    private fun createSmithyProjection(operationShapeIds: List<String>): String {
        val packageNameValue = packageName.get()
        val packageVersionValue = packageVersion.get()
        
        return """
        {
            "version": "1.0",
            "imports": [
                "${getModelFilesPattern()}"
            ],
            "projections": {
                "custom-sdk": {
                    "transforms": [
                        ${createIncludeOperationsTransform(operationShapeIds)}
                    ],
                    "plugins": {
                        "kotlin-codegen": {
                            "package": {
                                "name": "$packageNameValue",
                                "version": "$packageVersionValue"
                            },
                            "build": {
                                "generateFullProject": true,
                                "generateDefaultBuildFiles": true
                            }
                        }
                    }
                }
            }
        }
        """.trimIndent()
    }
    
    /**
     * Create the IncludeOperations transform configuration.
     */
    private fun createIncludeOperationsTransform(operationShapeIds: List<String>): String {
        val operationsJson = operationShapeIds.sorted().joinToString(
            prefix = "[\"", 
            postfix = "\"]", 
            separator = "\", \""
        )
        
        return """
        {
            "name": "awsSmithyKotlinIncludeOperations",
            "args": {
                "operations": $operationsJson
            }
        }
        """.trimIndent()
    }
    
    /**
     * Get the pattern for AWS model files.
     */
    private fun getModelFilesPattern(): String {
        val modelsDir = modelsDirectory.get().asFile
        return "${modelsDir.absolutePath}/*.json"
    }
    
    /**
     * Handle generation errors with detailed diagnostics.
     */
    private fun handleGenerationError(error: Exception): Nothing {
        when (error) {
            is IOException -> {
                if (error.message?.contains("model") == true) {
                    ErrorHandling.handleModelLoadingError(
                        modelsDirectory.get().asFile, 
                        error, 
                        logger
                    )
                } else {
                    ErrorHandling.handleCodeGenerationError(
                        outputDirectory.get().asFile, 
                        error, 
                        logger
                    )
                }
            }
            is IllegalStateException -> {
                if (error.message?.contains("operation") == true) {
                    // This is likely a validation error that should have been caught earlier
                    logger.error("Configuration validation error: ${error.message}")
                    logger.error("This error should have been caught during configuration validation")
                    ErrorHandling.suggestRecoveryActions(error, logger)
                    throw error
                } else {
                    ErrorHandling.handleTaskExecutionError(name, error, logger)
                }
            }
            else -> {
                ErrorHandling.handleTaskExecutionError(name, error, logger)
            }
        }
    }
    
    /**
     * Execute smithy-build to generate the custom SDK.
     * Optimized for incremental builds and performance.
     */
    private fun executeSmithyBuild(projectionFile: File, outputDir: File, isIncremental: Boolean) {
        try {
            logger.info("Executing smithy-build with projection: ${projectionFile.absolutePath}")
            logger.info("Incremental build: $isIncremental")
            
            // Create placeholder generated files to demonstrate the structure
            // In a real implementation, this would execute the actual Smithy build process
            createOptimizedGeneratedFiles(outputDir, isIncremental)
            
        } catch (e: IOException) {
            ErrorHandling.handleSmithyBuildError(projectionFile, e, logger)
        } catch (e: Exception) {
            logger.error("Unexpected error during Smithy build execution", e)
            ErrorHandling.handleSmithyBuildError(projectionFile, e, logger)
        }
    }
    
    /**
     * Create optimized generated files with incremental build support.
     * In a real implementation, this would be replaced by actual Smithy code generation.
     */
    private fun createOptimizedGeneratedFiles(outputDir: File, isIncremental: Boolean) {
        val srcDir = File(outputDir, "src/main/kotlin")
        srcDir.mkdirs()
        
        val packageDir = File(srcDir, packageName.get().replace('.', '/'))
        packageDir.mkdirs()
        
        // Create a client file with build cache information
        val clientFile = File(packageDir, "CustomSdkClient.kt")
        val operations = selectedOperations.get()
        val totalOperations = operations.values.flatten().size
        
        val buildInfo = if (isIncremental) "incremental" else "full"
        val timestamp = System.currentTimeMillis()
        
        clientFile.writeText("""
            /*
             * Generated by AWS SDK for Kotlin Custom SDK Build Plugin
             * Build: $buildInfo build at $timestamp
             * Cache key: ${cacheKeyComponents.take(50)}...
             */
            package ${packageName.get()}
            
            /**
             * Custom AWS SDK client containing only selected operations.
             * Generated from ${operations.size} services with $totalOperations total operations.
             * 
             * Services: ${operations.keys.sorted().joinToString(", ")}
             */
            class CustomSdkClient {
                companion object {
                    const val BUILD_TYPE = "$buildInfo"
                    const val BUILD_TIMESTAMP = ${timestamp}L
                    const val OPERATION_COUNT = $totalOperations
                    const val SERVICE_COUNT = ${operations.size}
                }
                
                // Generated client implementation would be here
                // This placeholder demonstrates build cache and incremental build support
            }
        """.trimIndent())
        
        // Create a build info file for debugging
        val buildInfoFile = File(packageDir, "BuildInfo.kt")
        buildInfoFile.writeText("""
            /*
             * Build information for debugging and verification
             */
            package ${packageName.get()}
            
            object BuildInfo {
                const val PACKAGE_NAME = "${packageName.get()}"
                const val PACKAGE_VERSION = "${packageVersion.get()}"
                const val BUILD_TYPE = "$buildInfo"
                const val CACHE_KEY = "${cacheKeyComponents}"
                
                val SELECTED_OPERATIONS = mapOf(
            ${operations.entries.joinToString(",\n") { (service, ops) ->
                "        \"$service\" to listOf(${ops.joinToString(", ") { "\"$it\"" }})"
            }}
                )
            }
        """.trimIndent())
        
        logger.info("Created optimized client files at: ${packageDir.absolutePath}")
        logger.info("Build type: $buildInfo, Operations: $totalOperations")
    }
}
