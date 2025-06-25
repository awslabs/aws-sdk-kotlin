/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import java.io.File

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
            
            // Register the generation task
            val generateTask = registerGenerationTask(project, extension)
            
            // Configure source sets and dependencies
            configureSourceSets(project, generateTask)
            
        } catch (e: Exception) {
            project.logger.error("Failed to configure Custom SDK Build plugin: ${e.message}")
            throw e
        }
    }
    
    /**
     * Register the custom SDK generation task.
     */
    private fun registerGenerationTask(
        project: Project, 
        extension: CustomSdkBuildExtension
    ): TaskProvider<GenerateCustomSdkTask> {
        // Create a separate task for preparing models
        val prepareModelsTask = project.tasks.register("prepareModels")
        prepareModelsTask.configure {
            doLast {
                val modelsDir = project.layout.buildDirectory.dir("models").get().asFile
                modelsDir.mkdirs()
                createPlaceholderModels(modelsDir)
            }
        }
        
        // Register the main generation task
        val generateTask = project.tasks.register("generateCustomSdk", GenerateCustomSdkTask::class.java)
        
        // Configure the task
        generateTask.configure {
            selectedOperations.set(extension.getSelectedOperations())
            packageName.set("aws.sdk.kotlin.services.custom")
            packageVersion.set(project.version.toString())
            modelsDirectory.set(project.layout.buildDirectory.dir("models"))
            dependsOn(prepareModelsTask)
        }
        
        return generateTask
    }
    
    /**
     * Configure source sets to include generated code.
     */
    private fun configureSourceSets(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        // This will be implemented in the next prompt
        project.logger.info("Source set configuration will be implemented in the next step")
    }
    
    /**
     * Create placeholder model files for demonstration.
     * In a real implementation, these would be the actual AWS service models.
     */
    private fun createPlaceholderModels(modelsDir: File) {
        // Create placeholder S3 model
        val s3Model = File(modelsDir, "s3.json")
        if (!s3Model.exists()) {
            s3Model.writeText("""
                {
                    "smithy": "2.0",
                    "metadata": {
                        "suppressions": []
                    },
                    "shapes": {
                        "com.amazonaws.s3#AmazonS3": {
                            "type": "service",
                            "version": "2006-03-01",
                            "operations": [
                                {
                                    "target": "com.amazonaws.s3#GetObject"
                                },
                                {
                                    "target": "com.amazonaws.s3#PutObject"
                                }
                            ],
                            "traits": {
                                "aws.api#service": {
                                    "sdkId": "S3"
                                }
                            }
                        },
                        "com.amazonaws.s3#GetObject": {
                            "type": "operation"
                        },
                        "com.amazonaws.s3#PutObject": {
                            "type": "operation"
                        }
                    }
                }
            """.trimIndent())
        }
        
        // Create placeholder DynamoDB model
        val dynamodbModel = File(modelsDir, "dynamodb.json")
        if (!dynamodbModel.exists()) {
            dynamodbModel.writeText("""
                {
                    "smithy": "2.0",
                    "metadata": {
                        "suppressions": []
                    },
                    "shapes": {
                        "com.amazonaws.dynamodb#DynamoDB_20120810": {
                            "type": "service",
                            "version": "2012-08-10",
                            "operations": [
                                {
                                    "target": "com.amazonaws.dynamodb#GetItem"
                                },
                                {
                                    "target": "com.amazonaws.dynamodb#PutItem"
                                }
                            ],
                            "traits": {
                                "aws.api#service": {
                                    "sdkId": "DynamoDB"
                                }
                            }
                        },
                        "com.amazonaws.dynamodb#GetItem": {
                            "type": "operation"
                        },
                        "com.amazonaws.dynamodb#PutItem": {
                            "type": "operation"
                        }
                    }
                }
            """.trimIndent())
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
