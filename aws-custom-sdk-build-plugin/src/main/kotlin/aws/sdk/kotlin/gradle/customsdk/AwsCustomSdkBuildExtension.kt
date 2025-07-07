/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Extension for configuring the AWS Custom SDK Build plugin.
 * 
 * Provides a type-safe DSL for specifying which AWS services and operations
 * to include in the custom SDK build.
 */
abstract class AwsCustomSdkBuildExtension(private val project: Project) {
    
    /**
     * AWS region for the generated clients (optional, defaults to us-east-1)
     */
    abstract val region: Property<String>
    
    /**
     * Output directory for generated clients (optional, defaults to build/generated/aws-custom-sdk)
     */
    abstract val outputDirectory: Property<String>
    
    /**
     * Package name for generated clients (optional, defaults to aws.sdk.kotlin.custom)
     */
    abstract val packageName: Property<String>
    
    /**
     * Enable strict validation of operations against generated constants (default: true)
     */
    abstract val strictValidation: Property<Boolean>
    
    /**
     * Set of selected services with their operations
     */
    private val selectedServices = mutableMapOf<String, MutableSet<String>>()
    
    init {
        // Set default values
        region.convention("us-east-1")
        outputDirectory.convention("${project.buildDir}/generated/aws-custom-sdk")
        packageName.convention("aws.sdk.kotlin.custom")
        strictValidation.convention(true)
    }
    
    /**
     * Configure a service with selected operations using type-safe constants
     * 
     * Example with type-safe constants:
     * ```
     * service("lambda") {
     *     operations(
     *         LambdaOperations.CreateFunction,
     *         LambdaOperations.InvokeFunction,
     *         LambdaOperations.DeleteFunction
     *     )
     * }
     * ```
     * 
     * Example with string literals (backward compatibility):
     * ```
     * service("s3") {
     *     operations("GetObject", "PutObject")
     * }
     * ```
     */
    fun service(serviceName: String, configure: ServiceConfiguration.() -> Unit) {
        val serviceConfig = ServiceConfiguration(serviceName, project)
        serviceConfig.configure()
        
        val operations = serviceConfig.getOperations()
        
        // Validate operations if strict validation is enabled
        if (strictValidation.get()) {
            val validationResult = ConstantsRegistry.validateOperations(serviceName, operations)
            if (!validationResult.isValid) {
                project.logger.warn("AWS Custom SDK: ${validationResult.message}")
                if (validationResult.invalidOperations.isNotEmpty()) {
                    project.logger.warn("Available operations for $serviceName: ${ConstantsRegistry.getServiceOperations(serviceName).sorted().joinToString(", ")}")
                }
            }
        }
        
        selectedServices[serviceName] = operations.toMutableSet()
    }
    
    /**
     * Configure multiple services using a DSL block
     * 
     * Example:
     * ```
     * services {
     *     lambda {
     *         operations(LambdaOperations.CreateFunction, LambdaOperations.InvokeFunction)
     *     }
     *     s3 {
     *         operations(S3Operations.GetObject, S3Operations.PutObject)
     *     }
     * }
     * ```
     */
    fun services(configure: ServicesConfiguration.() -> Unit) {
        val servicesConfig = ServicesConfiguration(this)
        servicesConfig.configure()
    }
    
    /**
     * Get all selected services and their operations
     */
    fun getSelectedServices(): Map<String, Set<String>> = selectedServices.toMap()
    
    /**
     * Get validation summary for all configured services
     */
    fun getValidationSummary(): Map<String, ConstantsRegistry.ValidationResult> {
        return selectedServices.mapValues { (serviceName, operations) ->
            ConstantsRegistry.validateOperations(serviceName, operations)
        }
    }
    
    /**
     * Configuration block for multiple services
     */
    class ServicesConfiguration(private val extension: AwsCustomSdkBuildExtension) {
        
        fun lambda(configure: ServiceConfiguration.() -> Unit) {
            extension.service("lambda", configure)
        }
        
        fun s3(configure: ServiceConfiguration.() -> Unit) {
            extension.service("s3", configure)
        }
        
        fun dynamodb(configure: ServiceConfiguration.() -> Unit) {
            extension.service("dynamodb", configure)
        }
        
        fun apigateway(configure: ServiceConfiguration.() -> Unit) {
            extension.service("apigateway", configure)
        }
        
        /**
         * Generic service configuration for services not explicitly defined
         */
        fun service(serviceName: String, configure: ServiceConfiguration.() -> Unit) {
            extension.service(serviceName, configure)
        }
    }
    
    /**
     * Configuration block for a specific service
     */
    class ServiceConfiguration(private val serviceName: String, private val project: Project) {
        private val operations = mutableSetOf<String>()
        
        /**
         * Add operations using type-safe constants or string literals
         * 
         * Examples:
         * ```
         * operations(LambdaOperations.CreateFunction, LambdaOperations.InvokeFunction)
         * operations("CreateFunction", "InvokeFunction")
         * ```
         */
        fun operations(vararg operationConstants: String) {
            operations.addAll(operationConstants)
        }
        
        /**
         * Add operations using a collection of constants
         */
        fun operations(operationConstants: Collection<String>) {
            operations.addAll(operationConstants)
        }
        
        /**
         * Add all available operations for this service
         */
        fun allOperations() {
            val availableOps = ConstantsRegistry.getServiceOperations(serviceName)
            if (availableOps.isNotEmpty()) {
                operations.addAll(availableOps)
                project.logger.info("Added all ${availableOps.size} operations for $serviceName service")
            } else {
                project.logger.warn("No operation constants available for $serviceName service")
            }
        }
        
        /**
         * Add operations matching a pattern (regex)
         */
        fun operationsMatching(pattern: String) {
            val regex = Regex(pattern)
            val availableOps = ConstantsRegistry.getServiceOperations(serviceName)
            val matchingOps = availableOps.filter { regex.matches(it) }
            operations.addAll(matchingOps)
            project.logger.info("Added ${matchingOps.size} operations matching '$pattern' for $serviceName service")
        }
        
        internal fun getOperations(): Set<String> = operations.toSet()
    }
}
