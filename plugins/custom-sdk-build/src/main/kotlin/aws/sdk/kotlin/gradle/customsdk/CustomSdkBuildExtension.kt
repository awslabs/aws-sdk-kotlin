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
     *     operations(DynamodbOperation.GetItem, DynamodbOperation.PutItem)
     * }
     * ```
     */
    fun dynamodb(configure: DynamodbServiceConfiguration.() -> Unit) {
        val config = DynamodbServiceConfiguration().apply(configure)
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
            config.selectedOperations.map { it.shapeId }
        }
    }
    
    /**
     * Validate the current configuration.
     * Throws an exception if the configuration is invalid.
     */
    internal fun validate() {
        if (serviceConfigurations.isEmpty()) {
            throw IllegalStateException("No services configured. Please configure at least one service.")
        }
        
        serviceConfigurations.forEach { (serviceName, config) ->
            if (config.selectedOperations.isEmpty()) {
                throw IllegalStateException("No operations selected for service '$serviceName'. Please select at least one operation.")
            }
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

/**
 * Base class for service configurations.
 */
abstract class ServiceConfiguration {
    internal val selectedOperations = mutableListOf<OperationConstant>()
}

/**
 * Configuration for Amazon S3 operations.
 */
class S3ServiceConfiguration : ServiceConfiguration() {
    fun operations(vararg operations: S3Operation) {
        selectedOperations.addAll(operations.map { it.constant })
    }
}

/**
 * Configuration for Amazon DynamoDB operations.
 */
class DynamodbServiceConfiguration : ServiceConfiguration() {
    fun operations(vararg operations: DynamodbOperation) {
        selectedOperations.addAll(operations.map { it.constant })
    }
}

/**
 * Configuration for AWS Lambda operations.
 */
class LambdaServiceConfiguration : ServiceConfiguration() {
    fun operations(vararg operations: LambdaOperation) {
        selectedOperations.addAll(operations.map { it.constant })
    }
}

/**
 * Represents an operation constant with its Smithy shape ID.
 */
data class OperationConstant(val shapeId: String) {
    override fun toString(): String = shapeId
}

/**
 * Sample operation constants for Amazon S3.
 */
enum class S3Operation(val constant: OperationConstant) {
    GetObject(OperationConstant("com.amazonaws.s3#GetObject")),
    PutObject(OperationConstant("com.amazonaws.s3#PutObject")),
    DeleteObject(OperationConstant("com.amazonaws.s3#DeleteObject")),
    ListObjects(OperationConstant("com.amazonaws.s3#ListObjects")),
    CreateBucket(OperationConstant("com.amazonaws.s3#CreateBucket"))
}

/**
 * Sample operation constants for Amazon DynamoDB.
 */
enum class DynamodbOperation(val constant: OperationConstant) {
    GetItem(OperationConstant("com.amazonaws.dynamodb#GetItem")),
    PutItem(OperationConstant("com.amazonaws.dynamodb#PutItem")),
    DeleteItem(OperationConstant("com.amazonaws.dynamodb#DeleteItem")),
    Query(OperationConstant("com.amazonaws.dynamodb#Query")),
    Scan(OperationConstant("com.amazonaws.dynamodb#Scan"))
}

/**
 * Sample operation constants for AWS Lambda.
 */
enum class LambdaOperation(val constant: OperationConstant) {
    Invoke(OperationConstant("com.amazonaws.lambda#Invoke")),
    CreateFunction(OperationConstant("com.amazonaws.lambda#CreateFunction")),
    DeleteFunction(OperationConstant("com.amazonaws.lambda#DeleteFunction")),
    ListFunctions(OperationConstant("com.amazonaws.lambda#ListFunctions")),
    UpdateFunctionCode(OperationConstant("com.amazonaws.lambda#UpdateFunctionCode"))
}
