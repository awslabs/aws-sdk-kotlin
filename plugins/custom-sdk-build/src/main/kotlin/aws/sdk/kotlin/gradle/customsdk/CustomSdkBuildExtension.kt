/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * Main extension for the Custom SDK Build plugin.
 * Provides the `awsCustomSdkBuild` DSL for users to configure custom SDK generation.
 */
open class CustomSdkBuildExtension(private val project: Project) {
    
    private val serviceConfigurations = mutableMapOf<String, ServiceConfiguration>()
    
    /**
     * Configure Amazon S3 operations.
     * Example:
     * ```
     * s3 {
     *     operations(S3Operation.GetObject, S3Operation.PutObject)
     * }
     * ```
     */
    fun s3(configure: S3ServiceConfiguration.() -> Unit) {
        val config = S3ServiceConfiguration().apply(configure)
        serviceConfigurations["s3"] = config
    }
    
    /**
     * Configure Amazon DynamoDB operations.
     * Example:
     * ```
     * dynamodb {
     *     operations(DynamoDbOperation.GetItem, DynamoDbOperation.PutItem)
     * }
     * ```
     */
    fun dynamodb(configure: DynamoDbServiceConfiguration.() -> Unit) {
        val config = DynamoDbServiceConfiguration().apply(configure)
        serviceConfigurations["dynamodb"] = config
    }
    
    /**
     * Configure Amazon Lambda operations.
     * Example:
     * ```
     * lambda {
     *     operations(LambdaOperation.Invoke, LambdaOperation.CreateFunction)
     * }
     * ```
     */
    fun lambda(configure: LambdaServiceConfiguration.() -> Unit) {
        val config = LambdaServiceConfiguration().apply(configure)
        serviceConfigurations["lambda"] = config
    }
    
    /**
     * Get all selected operations mapped by service name to operation shape IDs.
     * This is used internally by the plugin to generate the custom SDK.
     */
    internal fun getSelectedOperations(): Map<String, List<String>> {
        return serviceConfigurations.mapValues { (_, config) -> 
            config.getSelectedOperations()
        }
    }
    
    /**
     * Validate the current configuration.
     * Throws an exception if the configuration is invalid.
     */
    internal fun validate() {
        try {
            if (serviceConfigurations.isEmpty()) {
                throw IllegalStateException(
                    "No services configured for custom SDK generation. " +
                    "Add at least one service configuration using the DSL:\n" +
                    "awsCustomSdkBuild {\n" +
                    "    s3 {\n" +
                    "        operations(S3Operation.GetObject, S3Operation.PutObject)\n" +
                    "    }\n" +
                    "}"
                )
            }
            
            serviceConfigurations.forEach { (serviceName, config) ->
                if (config.selectedOperations.isEmpty()) {
                    throw IllegalStateException(
                        "No operations selected for service '$serviceName'. " +
                        "Add operations to your service configuration:\n" +
                        "$serviceName {\n" +
                        "    operations(/* operation constants */)\n" +
                        "}"
                    )
                }
                
                // Check for duplicate operations
                val operationIds = config.getSelectedOperations()
                val duplicates = operationIds.groupingBy { it }.eachCount().filter { it.value > 1 }
                if (duplicates.isNotEmpty()) {
                    project.logger.warn("Service '$serviceName' has duplicate operations: ${duplicates.keys}")
                }
            }
            
            val totalOperations = serviceConfigurations.values.sumOf { it.getSelectedOperations().size }
            project.logger.info("Extension validation passed: $totalOperations operations across ${serviceConfigurations.size} services")
            
        } catch (e: Exception) {
            project.logger.error("Extension validation failed: ${e.message}")
            ErrorHandling.suggestRecoveryActions(e, project.logger)
            throw e
        }
    }
    
    /**
     * Create a dependency notation for the generated custom SDK.
     * This returns a FileCollection that can be used in the dependencies block.
     */
    internal fun createDependencyNotation(): FileCollection {
        // Find the generation task
        val generateTask = project.tasks.findByName("generateCustomSdk") as? GenerateCustomSdkTask
        
        return if (generateTask != null) {
            // Return the generated source directory as a file collection
            // This will be compiled alongside user code via source set integration
            project.files(generateTask.outputDirectory.map { it.dir("src/main/kotlin") }).apply {
                // Ensure the generation task runs when this file collection is resolved
                builtBy(generateTask)
            }
        } else {
            // Return empty file collection if task not found (e.g., during testing)
            project.files()
        }
    }
}
