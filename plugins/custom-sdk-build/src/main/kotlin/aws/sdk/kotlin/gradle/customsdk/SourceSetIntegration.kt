/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Handles source set integration for generated custom SDK code.
 * Configures both JVM and multiplatform Kotlin projects to include generated sources.
 */
object SourceSetIntegration {
    
    /**
     * Configure source sets to include generated SDK code.
     * Supports both Kotlin JVM and Kotlin Multiplatform projects.
     */
    fun configureSourceSets(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        project.logger.info("Configuring source sets for custom SDK generation")
        
        // Configure for Kotlin Multiplatform projects
        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            configureMultiplatformSourceSets(project, generateTask)
        }
        
        // Configure for Kotlin JVM projects
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            configureJvmSourceSets(project, generateTask)
        }
        
        // Configure task dependencies for all Kotlin compilation tasks
        configureTaskDependencies(project, generateTask)
    }
    
    /**
     * Configure source sets for Kotlin Multiplatform projects.
     */
    private fun configureMultiplatformSourceSets(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        
        // Add generated sources to commonMain source set
        val commonMain = kotlin.sourceSets.getByName("commonMain")
        commonMain.kotlin.srcDir(generateTask.map { it.outputDirectory.dir("src/main/kotlin") })
        project.logger.info("Added custom SDK generated sources to commonMain source set")
    }
    
    /**
     * Configure source sets for Kotlin JVM projects.
     */
    private fun configureJvmSourceSets(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        val kotlin = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        
        // Add generated sources to main source set
        val main = kotlin.sourceSets.getByName("main")
        main.kotlin.srcDir(generateTask.map { it.outputDirectory.dir("src/main/kotlin") })
        project.logger.info("Added custom SDK generated sources to main source set")
    }
    
    /**
     * Configure task dependencies to ensure generation runs before Kotlin compilation.
     */
    private fun configureTaskDependencies(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        // Configure task dependencies after project evaluation
        project.afterEvaluate {
            // Make all Kotlin compilation tasks depend on the generation task
            project.tasks.withType(KotlinCompile::class.java).forEach { compileTask ->
                compileTask.dependsOn(generateTask)
                project.logger.debug("Configured ${compileTask.name} to depend on custom SDK generation")
            }
            
            // Also configure dependencies for common Gradle tasks by name
            project.tasks.findByName("compileKotlin")?.dependsOn(generateTask)
            project.tasks.findByName("compileTestKotlin")?.dependsOn(generateTask)
            
            // For multiplatform projects, configure additional compilation tasks
            project.tasks.names.filter { it.contains("compileKotlin") }.forEach { taskName ->
                project.tasks.findByName(taskName)?.dependsOn(generateTask)
            }
        }
    }
    
    /**
     * Configure IDE integration to ensure generated sources are visible in IDEs.
     */
    fun configureIdeIntegration(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        // Configure IDEA plugin if present
        project.plugins.withId("idea") {
            project.logger.info("Configuring IDEA integration for custom SDK sources")
            
            // The generated sources will be automatically picked up by IDEA
            // since they're added to the Kotlin source sets
        }
        
        // Configure Eclipse plugin if present
        project.plugins.withId("eclipse") {
            project.logger.info("Configuring Eclipse integration for custom SDK sources")
            
            // The generated sources will be automatically picked up by Eclipse
            // since they're added to the Kotlin source sets
        }
    }
    
    /**
     * Configure incremental build support.
     */
    fun configureIncrementalBuild(project: Project, generateTask: TaskProvider<GenerateCustomSdkTask>) {
        // The GenerateCustomSdkTask already has proper input/output annotations
        // This ensures incremental builds work correctly
        
        project.afterEvaluate {
            generateTask.get().outputs.upToDateWhen {
                // Task is up-to-date if inputs haven't changed
                // This is handled automatically by Gradle's input/output tracking
                true
            }
        }
        
        project.logger.info("Configured incremental build support for custom SDK generation")
    }
}
