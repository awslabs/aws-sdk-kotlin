/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

/**
 * Gradle plugin for generating custom AWS SDK clients containing only selected operations.
 * 
 * This plugin allows users to specify which AWS services and operations they need,
 * then generates a custom SDK client with reduced binary size and improved startup times.
 * 
 * Usage:
 * ```kotlin
 * plugins {
 *     id("aws.sdk.kotlin.custom-sdk-build")
 * }
 * 
 * val customSdk = awsCustomSdkBuild {
 *     s3 {
 *         operations(S3Operation.GetObject, S3Operation.PutObject)
 *     }
 *     
 *     dynamodb {
 *         operations(DynamodbOperation.GetItem, DynamodbOperation.PutItem)
 *     }
 * }
 * 
 * dependencies {
 *     implementation(customSdk)
 * }
 * ```
 */
class CustomSdkBuildPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        project.logger.info("Applying AWS SDK Kotlin Custom SDK Build plugin to project ${project.name}")
        
        // Register the extension
        val extension = project.extensions.create(
            "awsCustomSdkBuild", 
            CustomSdkBuildExtension::class.java, 
            project
        )
        
        // Configure the plugin after project evaluation
        project.afterEvaluate {
            configurePlugin(project, extension)
        }
    }
    
    /**
     * Configure the plugin after the project has been evaluated.
     * This is where we set up tasks and dependencies based on the user's configuration.
     */
    private fun configurePlugin(project: Project, extension: CustomSdkBuildExtension) {
        try {
            // Validate the extension configuration
            extension.validate()
            
            // Log the selected operations for debugging
            val selectedOperations = extension.getSelectedOperations()
            project.logger.info("Custom SDK configuration:")
            selectedOperations.forEach { (service, operations) ->
                project.logger.info("  $service: ${operations.size} operations")
                operations.forEach { operation ->
                    project.logger.debug("    - $operation")
                }
            }
            
            // TODO: In subsequent prompts, we'll add:
            // - Task registration for SDK generation
            // - Source set configuration
            // - Dependency management
            
        } catch (e: Exception) {
            project.logger.error("Failed to configure Custom SDK Build plugin: ${e.message}")
            throw e
        }
    }
}

/**
 * Extension function to create the awsCustomSdkBuild DSL and return a dependency notation.
 * This allows users to write:
 * 
 * ```kotlin
 * val customSdk = awsCustomSdkBuild { ... }
 * dependencies { implementation(customSdk) }
 * ```
 */
fun Project.awsCustomSdkBuild(configure: CustomSdkBuildExtension.() -> Unit): FileCollection {
    val extension = extensions.findByType(CustomSdkBuildExtension::class.java)
        ?: throw IllegalStateException("CustomSdkBuildExtension not found. Make sure the plugin is applied.")
    
    extension.configure()
    return extension.createDependencyNotation()
}
