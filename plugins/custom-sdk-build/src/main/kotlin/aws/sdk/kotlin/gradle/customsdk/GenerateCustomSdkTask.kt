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
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Task that generates custom SDK code using Smithy projections and transforms.
 * 
 * This task takes the user's operation selections and generates a filtered SDK
 * containing only the selected operations and their dependencies.
 */
@DisableCachingByDefault(because = "Custom SDK generation is not cacheable yet")
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
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val modelsDirectory: DirectoryProperty
    
    init {
        description = "Generates custom AWS SDK code with only selected operations"
        group = "aws-sdk-kotlin"
        
        // Set default values
        packageName.convention("aws.sdk.kotlin.services.custom")
        packageVersion.convention(project.version.toString())
        outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/customSdk"))
    }
    
    @TaskAction
    fun generate() {
        val operations = selectedOperations.get()
        val allOperationShapeIds = operations.values.flatten()
        
        logger.info("Generating custom SDK with ${allOperationShapeIds.size} operations across ${operations.size} services")
        
        if (allOperationShapeIds.isEmpty()) {
            throw IllegalStateException("No operations selected for custom SDK generation")
        }
        
        // Clean output directory
        val outputDir = outputDirectory.get().asFile
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()
        
        // Create Smithy projection configuration
        val projectionConfig = createSmithyProjection(allOperationShapeIds)
        
        // Write projection configuration to file
        val projectionFile = File(outputDir, "smithy-build.json")
        projectionFile.writeText(projectionConfig)
        
        logger.info("Created Smithy projection configuration at: ${projectionFile.absolutePath}")
        
        // Execute smithy-build to generate the custom SDK
        executeSmithyBuild(projectionFile, outputDir)
        
        logger.info("Custom SDK generation completed successfully")
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
        val operationsJson = operationShapeIds.joinToString(
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
     * Execute smithy-build to generate the custom SDK.
     */
    private fun executeSmithyBuild(projectionFile: File, outputDir: File) {
        try {
            // For now, we'll create a placeholder implementation
            // In a real implementation, this would execute the Smithy build process
            logger.info("Executing smithy-build with projection: ${projectionFile.absolutePath}")
            
            // Create placeholder generated files to demonstrate the structure
            createPlaceholderGeneratedFiles(outputDir)
            
        } catch (e: Exception) {
            logger.error("Failed to execute smithy-build", e)
            throw TaskExecutionException(this, e)
        }
    }
    
    /**
     * Create placeholder generated files to demonstrate the expected output structure.
     * In a real implementation, this would be replaced by actual Smithy code generation.
     */
    private fun createPlaceholderGeneratedFiles(outputDir: File) {
        val srcDir = File(outputDir, "src/main/kotlin")
        srcDir.mkdirs()
        
        val packageDir = File(srcDir, packageName.get().replace('.', '/'))
        packageDir.mkdirs()
        
        // Create a placeholder client file
        val clientFile = File(packageDir, "CustomSdkClient.kt")
        clientFile.writeText("""
            /*
             * Generated by AWS SDK for Kotlin Custom SDK Build Plugin
             */
            package ${packageName.get()}
            
            /**
             * Custom AWS SDK client containing only selected operations.
             * Generated from ${selectedOperations.get().size} services with ${selectedOperations.get().values.flatten().size} total operations.
             */
            class CustomSdkClient {
                // Generated client implementation would be here
            }
        """.trimIndent())
        
        logger.info("Created placeholder client at: ${clientFile.absolutePath}")
    }
}
