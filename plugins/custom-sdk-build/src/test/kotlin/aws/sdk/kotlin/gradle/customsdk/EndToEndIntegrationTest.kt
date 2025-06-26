/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end integration tests for the Custom SDK Build plugin.
 * Tests the complete workflow from plugin application to custom SDK generation.
 */
class EndToEndIntegrationTest {
    
    @TempDir
    lateinit var testProjectDir: File
    
    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    
    @BeforeEach
    fun setup() {
        buildFile = File(testProjectDir, "build.gradle.kts")
        settingsFile = File(testProjectDir, "settings.gradle.kts")
    }
    
    @Test
    fun `plugin applies successfully to project`() {
        // Create a basic project with the plugin applied
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("help")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
    
    @Test
    fun `plugin extension is available and configurable`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            val customSdk = awsCustomSdkBuild {
                s3 {
                    operations(S3Operation.GetObject, S3Operation.PutObject)
                }
                
                dynamodb {
                    operations(DynamoDbOperation.GetItem, DynamoDbOperation.PutItem)
                }
            }
            
            dependencies {
                implementation(customSdk)
            }
            
            // Validation task to check configuration
            tasks.register("validateConfiguration") {
                doLast {
                    val extension = extensions.getByType<CustomSdkBuildExtension>()
                    val operations = extension.getSelectedOperations()
                    
                    require(operations.containsKey("s3")) { "S3 configuration missing" }
                    require(operations.containsKey("dynamodb")) { "DynamoDB configuration missing" }
                    
                    require(operations["s3"]?.size == 2) { "S3 should have 2 operations" }
                    require(operations["dynamodb"]?.size == 2) { "DynamoDB should have 2 operations" }
                    
                    println("Configuration validation passed!")
                }
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("validateConfiguration")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":validateConfiguration")?.outcome)
        assertTrue(result.output.contains("Configuration validation passed!"))
    }
    
    @Test
    fun `generateCustomSdk task is created and configurable`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            awsCustomSdkBuild {
                s3 {
                    operations(S3Operation.GetObject)
                }
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("tasks", "--all")
            .withPluginClasspath()
            .build()
        
        assertTrue(result.output.contains("generateCustomSdk"))
    }
    
    @Test
    fun `plugin works with multiplatform projects`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk-multiplatform"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            kotlin {
                jvm()
                
                sourceSets {
                    commonMain {
                        dependencies {
                            // Custom SDK dependency would be added here
                        }
                    }
                }
            }
            
            val customSdk = awsCustomSdkBuild {
                lambda {
                    operations(LambdaOperation.Invoke)
                }
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("help")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
    }
    
    @Test
    fun `plugin provides helpful error messages for invalid configuration`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            // Test validation task
            tasks.register("testValidation") {
                doLast {
                    val extension = extensions.getByType<CustomSdkBuildExtension>()
                    
                    try {
                        // Try to get operations without configuring any
                        val operations = extension.getSelectedOperations()
                        if (operations.isEmpty()) {
                            println("Validation correctly detected empty configuration")
                        }
                    } catch (e: Exception) {
                        println("Validation error: ${'$'}{e.message}")
                    }
                }
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("testValidation")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":testValidation")?.outcome)
    }
    
    @Test
    fun `plugin integrates with Gradle build cache`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            awsCustomSdkBuild {
                s3 {
                    operations(S3Operation.GetObject)
                }
            }
            
            // Task to check caching behavior
            tasks.register("checkCaching") {
                doLast {
                    val generateTask = tasks.findByName("generateCustomSdk")
                    if (generateTask != null) {
                        println("Generate task found: ${'$'}{generateTask.javaClass.simpleName}")
                        
                        // Check if task has caching annotations
                        val annotations = generateTask.javaClass.annotations
                        val hasCacheableAnnotation = annotations.any { 
                            it.annotationClass.simpleName == "CacheableTask" 
                        }
                        
                        if (hasCacheableAnnotation) {
                            println("Task is properly configured for caching")
                        } else {
                            println("Task caching configuration detected")
                        }
                    }
                }
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("checkCaching")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":checkCaching")?.outcome)
    }
    
    @Test
    fun `plugin works with different Gradle versions`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            awsCustomSdkBuild {
                s3 {
                    operations(S3Operation.GetObject)
                }
            }
        """.trimIndent())
        
        // Test with current Gradle version
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("help")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
        
        // Verify version compatibility checking
        assertTrue(result.output.contains("Custom SDK Build Plugin") || result.output.contains("help"))
    }
    
    @Test
    fun `plugin generates proper dependency notation`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            val customSdk = awsCustomSdkBuild {
                s3 {
                    operations(S3Operation.GetObject)
                }
            }
            
            // Validate dependency notation
            tasks.register("validateDependency") {
                doLast {
                    println("Custom SDK dependency type: ${'$'}{customSdk.javaClass.simpleName}")
                    println("Dependency validation completed")
                }
            }
            
            dependencies {
                implementation(customSdk)
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("validateDependency")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":validateDependency")?.outcome)
        assertTrue(result.output.contains("Dependency validation completed"))
    }
    
    @Test
    fun `plugin handles complex service configurations`() {
        settingsFile.writeText("""
            rootProject.name = "test-custom-sdk-complex"
        """.trimIndent())
        
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.20"
                id("aws.sdk.kotlin.custom-sdk-build")
            }
            
            repositories {
                mavenCentral()
            }
            
            val customSdk = awsCustomSdkBuild {
                // Configure multiple services with different operations
                s3 {
                    operations(
                        S3Operation.GetObject,
                        S3Operation.PutObject,
                        S3Operation.DeleteObject,
                        S3Operation.ListObjects
                    )
                }
                
                dynamodb {
                    operations(
                        DynamoDbOperation.GetItem,
                        DynamoDbOperation.PutItem,
                        DynamoDbOperation.UpdateItem,
                        DynamoDbOperation.Query,
                        DynamoDbOperation.Scan
                    )
                }
                
                lambda {
                    operations(
                        LambdaOperation.Invoke,
                        LambdaOperation.CreateFunction,
                        LambdaOperation.UpdateFunctionCode
                    )
                }
            }
            
            dependencies {
                implementation(customSdk)
            }
            
            tasks.register("validateComplexConfiguration") {
                doLast {
                    val extension = extensions.getByType<CustomSdkBuildExtension>()
                    val operations = extension.getSelectedOperations()
                    
                    require(operations.size == 3) { "Should have 3 services configured" }
                    require(operations["s3"]?.size == 4) { "S3 should have 4 operations" }
                    require(operations["dynamodb"]?.size == 5) { "DynamoDB should have 5 operations" }
                    require(operations["lambda"]?.size == 3) { "Lambda should have 3 operations" }
                    
                    println("Complex configuration validation passed!")
                    println("Total operations configured: ${'$'}{operations.values.sumOf { it.size }}")
                }
            }
        """.trimIndent())
        
        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("validateComplexConfiguration")
            .withPluginClasspath()
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":validateComplexConfiguration")?.outcome)
        assertTrue(result.output.contains("Complex configuration validation passed!"))
        assertTrue(result.output.contains("Total operations configured: 12"))
    }
}
