/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task that generates custom AWS service clients with only selected operations.
 * 
 * This task orchestrates the entire custom client generation process:
 * 1. Validates the configuration
 * 2. Sets up Smithy build configuration
 * 3. Generates filtered service clients
 * 4. Copies generated artifacts to the output directory
 */
abstract class GenerateCustomClientsTask : DefaultTask() {
    
    @get:Input
    val selectedServices: Map<String, Set<String>>
        get() = getExtension().getSelectedServices()
    
    @get:Input
    val packageName: String
        get() = getExtension().packageName.get()
    
    @get:Input
    val region: String
        get() = getExtension().region.get()
    
    @get:OutputDirectory
    val outputDirectory: File
        get() = File(getExtension().outputDirectory.get())
    
    private fun getExtension(): AwsCustomSdkBuildExtension {
        return project.extensions.getByType(AwsCustomSdkBuildExtension::class.java)
    }
    
    @TaskAction
    fun generateClients() {
        logger.info("Starting AWS Custom SDK client generation...")
        
        val extension = getExtension()
        
        // Validate configuration
        validateConfiguration(extension)
        
        // Generate validation summary
        logValidationSummary(extension)
        
        // Configure dependencies
        configureDependencies(extension)
        
        // Generate clients using Smithy build
        val smithyConfigurator = SmithyBuildConfigurator(project, extension)
        val buildDir = smithyConfigurator.generateCustomClients()
        
        // Copy generated clients to output directory
        copyGeneratedClients(buildDir, smithyConfigurator)
        
        // Generate usage examples
        generateUsageExamples(extension)
        
        logger.info("AWS Custom SDK client generation completed successfully")
        logger.info("Generated clients available at: ${outputDirectory.absolutePath}")
    }
    
    /**
     * Validate the plugin configuration
     */
    private fun validateConfiguration(extension: AwsCustomSdkBuildExtension) {
        val selectedServices = extension.getSelectedServices()
        
        if (selectedServices.isEmpty()) {
            throw IllegalStateException(
                "No services configured for custom SDK generation. " +
                "Please configure at least one service using the awsCustomSdk DSL."
            )
        }
        
        // Validate that all services have at least one operation
        selectedServices.forEach { (serviceName, operations) ->
            if (operations.isEmpty()) {
                throw IllegalStateException(
                    "Service '$serviceName' has no operations configured. " +
                    "Please specify at least one operation for each service."
                )
            }
        }
        
        // Validate output directory
        val outputDir = File(extension.outputDirectory.get())
        if (outputDir.exists() && !outputDir.isDirectory) {
            throw IllegalStateException(
                "Output path exists but is not a directory: ${outputDir.absolutePath}"
            )
        }
        
        logger.info("Configuration validation passed")
    }
    
    /**
     * Log validation summary for all configured services
     */
    private fun logValidationSummary(extension: AwsCustomSdkBuildExtension) {
        logger.info("=== AWS Custom SDK Configuration Summary ===")
        logger.info("Package Name: ${extension.packageName.get()}")
        logger.info("Region: ${extension.region.get()}")
        logger.info("Output Directory: ${extension.outputDirectory.get()}")
        logger.info("Strict Validation: ${extension.strictValidation.get()}")
        
        val validationSummary = extension.getValidationSummary()
        val selectedServices = extension.getSelectedServices()
        
        logger.info("Services and Operations:")
        selectedServices.forEach { (serviceName, operations) ->
            logger.info("  $serviceName (${operations.size} operations):")
            
            val validation = validationSummary[serviceName]
            if (validation != null && !validation.isValid) {
                logger.warn("    ⚠️  ${validation.message}")
                if (validation.invalidOperations.isNotEmpty()) {
                    logger.warn("    Invalid: ${validation.invalidOperations.joinToString(", ")}")
                }
            }
            
            // Log first few operations
            operations.take(5).forEach { operation ->
                logger.info("    - $operation")
            }
            if (operations.size > 5) {
                logger.info("    ... and ${operations.size - 5} more operations")
            }
        }
        logger.info("=== End Configuration Summary ===")
    }
    
    /**
     * Configure project dependencies based on selected services
     */
    private fun configureDependencies(extension: AwsCustomSdkBuildExtension) {
        logger.info("Configuring dependencies for selected services...")
        
        val dependencyManager = DependencyManager(project)
        dependencyManager.configureDependencies(extension.getSelectedServices())
        
        logger.info("Dependencies configured successfully")
    }
    
    /**
     * Copy generated clients from Smithy build output to the configured output directory
     */
    private fun copyGeneratedClients(buildDir: File, smithyConfigurator: SmithyBuildConfigurator) {
        logger.info("Copying generated clients to output directory...")
        
        val generatedClientsDir = smithyConfigurator.getGeneratedClientsDir(buildDir)
        
        if (!generatedClientsDir.exists()) {
            throw RuntimeException("Generated clients directory not found: ${generatedClientsDir.absolutePath}")
        }
        
        // Ensure output directory exists
        outputDirectory.mkdirs()
        
        // Copy each service's generated code
        val selectedServices = getExtension().getSelectedServices()
        selectedServices.keys.forEach { serviceName ->
            val serviceDir = File(generatedClientsDir, serviceName)
            if (serviceDir.exists()) {
                val targetDir = File(outputDirectory, serviceName)
                serviceDir.copyRecursively(targetDir, overwrite = true)
                logger.info("Copied $serviceName client to: ${targetDir.absolutePath}")
            } else {
                logger.warn("Generated client not found for service: $serviceName")
            }
        }
        
        logger.info("Client copying completed")
    }
    
    /**
     * Generate usage examples and documentation
     */
    private fun generateUsageExamples(extension: AwsCustomSdkBuildExtension) {
        logger.info("Generating usage examples...")
        
        val examplesDir = File(outputDirectory, "examples")
        examplesDir.mkdirs()
        
        val selectedServices = extension.getSelectedServices()
        val packageName = extension.packageName.get()
        val region = extension.region.get()
        
        // Generate a comprehensive usage example
        val exampleContent = generateUsageExampleContent(selectedServices, packageName, region)
        val exampleFile = File(examplesDir, "CustomSdkUsageExample.kt")
        exampleFile.writeText(exampleContent)
        
        // Generate README
        val readmeContent = generateReadmeContent(selectedServices, packageName)
        val readmeFile = File(outputDirectory, "README.md")
        readmeFile.writeText(readmeContent)
        
        logger.info("Usage examples generated at: ${examplesDir.absolutePath}")
    }
    
    /**
     * Generate usage example Kotlin code
     */
    private fun generateUsageExampleContent(
        selectedServices: Map<String, Set<String>>,
        packageName: String,
        region: String
    ): String {
        val content = StringBuilder()
        
        content.append("""
            /*
             * Custom AWS SDK Usage Example
             * Generated by AWS Custom SDK Build Plugin
             */
            
            package $packageName.examples
            
            import kotlinx.coroutines.runBlocking
            
        """.trimIndent())
        
        // Add imports for each service
        selectedServices.keys.forEach { serviceName ->
            content.append("import $packageName.services.$serviceName.${serviceName.replaceFirstChar { it.uppercase() }}Client\n")
        }
        
        content.append("""
            
            /**
             * Example demonstrating usage of the custom AWS SDK clients.
             * This example shows how to use the generated clients with only the selected operations.
             */
            suspend fun main() {
                println("Custom AWS SDK Example")
                println("Region: $region")
                println("Services: ${selectedServices.keys.joinToString(", ")}")
                
        """.trimIndent())
        
        // Generate example usage for each service
        selectedServices.forEach { (serviceName, operations) ->
            val clientName = "${serviceName.replaceFirstChar { it.uppercase() }}Client"
            val firstOperation = operations.first()
            
            content.append("""
                
                // $serviceName Service Example
                val ${serviceName}Client = $clientName {
                    region = "$region"
                }
                
                try {
                    // Example: $firstOperation operation
                    // val response = ${serviceName}Client.${firstOperation.replaceFirstChar { it.lowercase() }}(request)
                    println("$serviceName client initialized successfully")
                } catch (e: Exception) {
                    println("Error with $serviceName client: ${'$'}{e.message}")
                } finally {
                    ${serviceName}Client.close()
                }
            """.trimIndent())
        }
        
        content.append("""
            
                println("Custom AWS SDK example completed")
            }
        """.trimIndent())
        
        return content.toString()
    }
    
    /**
     * Generate README content for the custom SDK
     */
    private fun generateReadmeContent(
        selectedServices: Map<String, Set<String>>,
        packageName: String
    ): String {
        return """
            # Custom AWS SDK for Kotlin
            
            This custom AWS SDK was generated with only the operations you need, resulting in a smaller and more focused SDK.
            
            ## Generated Services
            
            ${selectedServices.map { (serviceName, operations) ->
                "- **${serviceName.uppercase()}**: ${operations.size} operations\n  - ${operations.take(5).joinToString(", ")}${if (operations.size > 5) ", ..." else ""}"
            }.joinToString("\n")}
            
            ## Usage
            
            Add the generated clients to your project and use them like the standard AWS SDK:
            
            ```kotlin
            import $packageName.services.lambda.LambdaClient
            
            val lambdaClient = LambdaClient {
                region = "us-east-1"
            }
            
            // Use only the operations you configured
            val response = lambdaClient.createFunction(request)
            ```
            
            ## Package Structure
            
            ```
            $packageName/
            ├── services/
            ${selectedServices.keys.map { "│   ├── $it/" }.joinToString("\n")}
            └── examples/
                └── CustomSdkUsageExample.kt
            ```
            
            ## Benefits
            
            - **Smaller Size**: Only includes the operations you need
            - **Faster Builds**: Reduced compilation time
            - **Type Safety**: Full Kotlin type safety maintained
            - **Drop-in Replacement**: Same API as the full AWS SDK
            
            Generated by AWS Custom SDK Build Plugin
        """.trimIndent()
    }
}
