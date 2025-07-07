/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for generating lightweight AWS service clients with only selected operations.
 * 
 * This plugin provides a DSL for configuring which AWS services and operations to include
 * in a custom SDK build, automatically managing dependencies and generating type-safe
 * operation constants for configuration.
 */
class AwsCustomSdkBuildPlugin : Plugin<Project> {
    
    companion object {
        const val EXTENSION_NAME = "awsCustomSdk"
        const val GENERATE_CLIENTS_TASK_NAME = "generateAwsCustomClients"
    }
    
    override fun apply(project: Project) {
        // Create the extension for DSL configuration
        val extension = project.extensions.create(
            EXTENSION_NAME,
            AwsCustomSdkBuildExtension::class.java,
            project
        )
        
        // Register the main task for generating custom clients
        project.tasks.register(GENERATE_CLIENTS_TASK_NAME, GenerateCustomClientsTask::class.java)
        
        // Configure dependencies after evaluation
        project.afterEvaluate {
            configureDependencies(project, extension)
        }
    }
    
    /**
     * Configure project dependencies based on selected services
     */
    private fun configureDependencies(project: Project, extension: AwsCustomSdkBuildExtension) {
        val dependencyManager = DependencyManager(project)
        dependencyManager.configureDependencies(extension.getSelectedServices())
    }
}
