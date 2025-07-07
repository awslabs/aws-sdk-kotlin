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
        // Protocol to dependency mapping
        private val PROTOCOL_DEPENDENCIES = mapOf(
            "json" to listOf(
                "aws.sdk.kotlin:aws-json",
                "software.amazon.smithy.kotlin:http-client-engine-default"
            ),
            "xml" to listOf(
                "aws.sdk.kotlin:aws-xml", 
                "software.amazon.smithy.kotlin:http-client-engine-default"
            ),
            "query" to listOf(
                "aws.sdk.kotlin:aws-query",
                "software.amazon.smithy.kotlin:http-client-engine-default"
            ),
            "rest" to listOf(
                "aws.sdk.kotlin:aws-core",
                "software.amazon.smithy.kotlin:http-client-engine-default"
            )
        )
        
        // Service to protocol mapping (will be populated from service models in later prompts)
        private val SERVICE_PROTOCOLS = mapOf(
            "s3" to "rest",
            "dynamodb" to "json",
            "ec2" to "query",
            "lambda" to "json",
            "sns" to "query",
            "sqs" to "query"
            // TODO: Complete mapping will be loaded from service models
        )
    }
    
    /**
     * Configure dependencies for the selected services
     */
    fun configureDependencies(selectedServices: Map<String, Set<String>>) {
        project.logger.info("Configuring dependencies for ${selectedServices.size} services...")
        
        val requiredProtocols = determineRequiredProtocols(selectedServices.keys)
        val dependencies = resolveDependencies(requiredProtocols)
        
        addDependenciesToProject(dependencies)
        
        project.logger.info("Added ${dependencies.size} dependencies for protocols: ${requiredProtocols.joinToString(", ")}")
    }
    
    /**
     * Determine which protocols are needed based on selected services
     */
    private fun determineRequiredProtocols(serviceNames: Set<String>): Set<String> {
        val protocols = mutableSetOf<String>()
        
        serviceNames.forEach { serviceName ->
            val protocol = SERVICE_PROTOCOLS[serviceName]
            if (protocol != null) {
                protocols.add(protocol)
            } else {
                project.logger.warn("Unknown protocol for service: $serviceName, defaulting to 'rest'")
                protocols.add("rest")
            }
        }
        
        return protocols
    }
    
    /**
     * Resolve dependencies for the required protocols
     */
    private fun resolveDependencies(protocols: Set<String>): Set<String> {
        val dependencies = mutableSetOf<String>()
        
        protocols.forEach { protocol ->
            val protocolDeps = PROTOCOL_DEPENDENCIES[protocol]
            if (protocolDeps != null) {
                dependencies.addAll(protocolDeps)
            }
        }
        
        // Always add core AWS runtime dependencies
        dependencies.addAll(listOf(
            "aws.sdk.kotlin:aws-core",
            "aws.sdk.kotlin:aws-config",
            "software.amazon.smithy.kotlin:smithy-client"
        ))
        
        return dependencies
    }
    
    /**
     * Add dependencies to the project configuration
     */
    private fun addDependenciesToProject(dependencies: Set<String>) {
        val implementation = project.configurations.getByName("implementation")
        
        dependencies.forEach { dependency ->
            project.logger.debug("Adding dependency: $dependency")
            project.dependencies.add("implementation", dependency)
        }
    }
}
