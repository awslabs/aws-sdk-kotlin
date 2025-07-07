/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Registry for managing and validating AWS service operation constants.
 * 
 * This class provides functionality to:
 * - Discover available service operation constants
 * - Validate operation names against known constants
 * - Provide type-safe access to operation constants
 */
object ConstantsRegistry {
    
    private val serviceConstants = mutableMapOf<String, Set<String>>()
    
    init {
        // Register known service constants
        registerServiceConstants()
    }
    
    /**
     * Register all available service operation constants
     */
    private fun registerServiceConstants() {
        try {
            // Register Lambda operations
            registerService("lambda", aws.sdk.kotlin.gradle.customsdk.constants.LambdaOperations::class)
            
            // Register S3 operations
            registerService("s3", aws.sdk.kotlin.gradle.customsdk.constants.S3Operations::class)
            
            // Register DynamoDB operations
            registerService("dynamodb", aws.sdk.kotlin.gradle.customsdk.constants.DynamoDbOperations::class)
            
            // Register API Gateway operations
            registerService("apigateway", aws.sdk.kotlin.gradle.customsdk.constants.ApiGatewayOperations::class)
            
        } catch (e: Exception) {
            // If constants are not available, continue without them
            // This allows the plugin to work even if constants haven't been generated yet
            println("Warning: Some operation constants are not available: ${e.message}")
        }
    }
    
    /**
     * Register operations for a specific service using reflection
     */
    private fun registerService(serviceName: String, constantsClass: KClass<*>) {
        try {
            val operations = mutableSetOf<String>()
            
            // Use reflection to get all const val properties
            constantsClass.memberProperties.forEach { property ->
                if (property.isConst) {
                    property.isAccessible = true
                    val value = property.getter.call()
                    if (value is String) {
                        operations.add(value)
                    }
                }
            }
            
            serviceConstants[serviceName.lowercase()] = operations
            println("Registered ${operations.size} operations for $serviceName service")
            
        } catch (e: Exception) {
            println("Warning: Could not register constants for $serviceName: ${e.message}")
        }
    }
    
    /**
     * Get all available operations for a service
     */
    fun getServiceOperations(serviceName: String): Set<String> {
        return serviceConstants[serviceName.lowercase()] ?: emptySet()
    }
    
    /**
     * Check if an operation is valid for a service
     */
    fun isValidOperation(serviceName: String, operationName: String): Boolean {
        val serviceOps = getServiceOperations(serviceName)
        return serviceOps.isEmpty() || serviceOps.contains(operationName)
    }
    
    /**
     * Get all registered services
     */
    fun getRegisteredServices(): Set<String> {
        return serviceConstants.keys
    }
    
    /**
     * Validate a list of operations for a service
     */
    fun validateOperations(serviceName: String, operations: Collection<String>): ValidationResult {
        val serviceOps = getServiceOperations(serviceName)
        
        // If no constants are registered for this service, allow all operations
        if (serviceOps.isEmpty()) {
            return ValidationResult(
                isValid = true,
                validOperations = operations.toSet(),
                invalidOperations = emptySet(),
                message = "No validation constraints available for service: $serviceName"
            )
        }
        
        val validOps = operations.filter { serviceOps.contains(it) }.toSet()
        val invalidOps = operations.filter { !serviceOps.contains(it) }.toSet()
        
        return ValidationResult(
            isValid = invalidOps.isEmpty(),
            validOperations = validOps,
            invalidOperations = invalidOps,
            message = if (invalidOps.isEmpty()) {
                "All operations are valid"
            } else {
                "Invalid operations for $serviceName: ${invalidOps.joinToString(", ")}"
            }
        )
    }
    
    /**
     * Result of operation validation
     */
    data class ValidationResult(
        val isValid: Boolean,
        val validOperations: Set<String>,
        val invalidOperations: Set<String>,
        val message: String
    )
}
