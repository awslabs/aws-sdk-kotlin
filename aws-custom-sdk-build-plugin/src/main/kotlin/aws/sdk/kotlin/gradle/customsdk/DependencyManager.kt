/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Manages automatic dependency resolution for AWS services based on their protocols.
 * 
 * This class analyzes the selected services and automatically adds the required
 * protocol-specific dependencies (JSON, XML, etc.) to the project configuration.
 */
class DependencyManager(private val project: Project) {
    companion object {
        // Core AWS SDK dependencies that are always needed
        private val CORE_DEPENDENCIES = listOf(
            "aws.sdk.kotlin:aws-core",
            "aws.sdk.kotlin:aws-config", 
            "aws.sdk.kotlin:aws-http",
            "aws.sdk.kotlin:aws-endpoint",
            "software.amazon.smithy.kotlin:http-client-engine-default",
            "software.amazon.smithy.kotlin:runtime-core"
        )
        
        // Protocol to dependency mapping
        private val PROTOCOL_DEPENDENCIES = mapOf(
            "json" to listOf(
                "software.amazon.smithy.kotlin:serde-json"
            ),
            "xml" to listOf(
                "software.amazon.smithy.kotlin:serde-xml"
            ),
            "query" to listOf(
                "software.amazon.smithy.kotlin:serde-form-url"
            ),
            "rest" to listOf(
                "software.amazon.smithy.kotlin:http"
            ),
            "cbor" to listOf(
                "software.amazon.smithy.kotlin:serde-cbor"
            )
        )
        
        // Service to protocol mapping based on AWS service protocols
        private val SERVICE_PROTOCOLS = mapOf(
            "s3" to setOf("rest", "xml"),
            "dynamodb" to setOf("json"),
            "ec2" to setOf("query"),
            "lambda" to setOf("json"),
            "sns" to setOf("query"),
            "sqs" to setOf("query"),
            "iam" to setOf("query"),
            "cloudformation" to setOf("query"),
            "rds" to setOf("query"),
            "apigateway" to setOf("rest", "json"),
            "cloudwatch" to setOf("query"),
            "sts" to setOf("query"),
            "secretsmanager" to setOf("json"),
            "ssm" to setOf("json"),
            "kinesis" to setOf("json"),
            "firehose" to setOf("json"),
            "cognito-identity" to setOf("json"),
            "cognito-idp" to setOf("json")
        )
        
        // AWS SDK version - should match the parent project
        private const val AWS_SDK_VERSION = "1.4.119-SNAPSHOT"
        private const val SMITHY_KOTLIN_VERSION = "0.34.21"
    }
    
    /**
     * Configure dependencies for the selected services
     */
    fun configureDependencies(selectedServices: Map<String, Set<String>>) {
        project.logger.info("Configuring dependencies for ${selectedServices.size} services...")
        
        // Get or create the implementation configuration
        val implementationConfig = project.configurations.findByName("implementation")
            ?: project.configurations.create("implementation")
        
        // Add core dependencies
        addCoreDependencies(implementationConfig)
        
        // Add protocol-specific dependencies based on selected services
        val requiredProtocols = determineRequiredProtocols(selectedServices.keys)
        addProtocolDependencies(implementationConfig, requiredProtocols)
        
        // Add service-specific dependencies if needed
        addServiceSpecificDependencies(implementationConfig, selectedServices.keys)
        
        project.logger.info("Dependencies configured successfully")
    }
    
    /**
     * Add core AWS SDK dependencies that are always required
     */
    private fun addCoreDependencies(config: Configuration) {
        project.logger.info("Adding core AWS SDK dependencies...")
        
        CORE_DEPENDENCIES.forEach { dependency ->
            val dependencyNotation = if (dependency.startsWith("aws.sdk.kotlin:")) {
                "$dependency:$AWS_SDK_VERSION"
            } else {
                "$dependency:$SMITHY_KOTLIN_VERSION"
            }
            
            project.dependencies.add(config.name, dependencyNotation)
            project.logger.debug("Added core dependency: $dependencyNotation")
        }
    }
    
    /**
     * Determine which protocols are required based on the selected services
     */
    private fun determineRequiredProtocols(serviceNames: Set<String>): Set<String> {
        val requiredProtocols = mutableSetOf<String>()
        
        serviceNames.forEach { serviceName ->
            val protocols = SERVICE_PROTOCOLS[serviceName.lowercase()]
            if (protocols != null) {
                requiredProtocols.addAll(protocols)
                project.logger.debug("Service $serviceName requires protocols: ${protocols.joinToString(", ")}")
            } else {
                // Default to JSON and REST for unknown services
                requiredProtocols.addAll(setOf("json", "rest"))
                project.logger.warn("Unknown service $serviceName, using default protocols: json, rest")
            }
        }
        
        project.logger.info("Required protocols: ${requiredProtocols.joinToString(", ")}")
        return requiredProtocols
    }
    
    /**
     * Add protocol-specific dependencies
     */
    private fun addProtocolDependencies(config: Configuration, requiredProtocols: Set<String>) {
        project.logger.info("Adding protocol-specific dependencies...")
        
        requiredProtocols.forEach { protocol ->
            val dependencies = PROTOCOL_DEPENDENCIES[protocol]
            if (dependencies != null) {
                dependencies.forEach { dependency ->
                    val dependencyNotation = "$dependency:$SMITHY_KOTLIN_VERSION"
                    project.dependencies.add(config.name, dependencyNotation)
                    project.logger.debug("Added protocol dependency for $protocol: $dependencyNotation")
                }
            }
        }
    }
    
    /**
     * Add service-specific dependencies if needed
     */
    private fun addServiceSpecificDependencies(config: Configuration, serviceNames: Set<String>) {
        project.logger.info("Checking for service-specific dependencies...")
        
        serviceNames.forEach { serviceName ->
            when (serviceName.lowercase()) {
                "s3" -> {
                    // S3 might need additional dependencies for multipart uploads, etc.
                    project.logger.debug("S3 service detected - using standard dependencies")
                }
                "dynamodb" -> {
                    // DynamoDB might need additional dependencies for enhanced client features
                    project.logger.debug("DynamoDB service detected - using standard dependencies")
                }
                "lambda" -> {
                    // Lambda might need additional dependencies for async invocation
                    project.logger.debug("Lambda service detected - using standard dependencies")
                }
                else -> {
                    project.logger.debug("Service $serviceName - using standard dependencies")
                }
            }
        }
    }
    
    /**
     * Get a summary of configured dependencies
     */
    fun getDependencySummary(selectedServices: Map<String, Set<String>>): String {
        val summary = StringBuilder()
        summary.append("Dependency Configuration Summary:\n")
        summary.append("- Core Dependencies: ${CORE_DEPENDENCIES.size}\n")
        
        val requiredProtocols = determineRequiredProtocols(selectedServices.keys)
        val protocolDependencies = requiredProtocols.flatMap { protocol ->
            PROTOCOL_DEPENDENCIES[protocol] ?: emptyList()
        }
        summary.append("- Protocol Dependencies: ${protocolDependencies.size} (${requiredProtocols.joinToString(", ")})\n")
        summary.append("- Services: ${selectedServices.keys.joinToString(", ")}\n")
        
        return summary.toString()
    }
}
