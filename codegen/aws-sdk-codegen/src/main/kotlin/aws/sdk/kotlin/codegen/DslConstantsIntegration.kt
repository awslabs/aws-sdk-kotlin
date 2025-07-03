/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.utils.toPascalCase
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import java.io.File

/**
 * Generates DSL operation constants for the AWS Custom SDK Build plugin.
 * 
 * This integration runs during the existing ./gradlew bootstrap process and generates
 * type-safe operation constants (e.g., S3Operations.GetObject) that can be used
 * in the plugin's DSL configuration.
 * 
 * The constants are generated per-service and copied to the plugin's resources
 * directory for distribution.
 */
class DslConstantsIntegration : KotlinIntegration {
    
    companion object {
        const val INTEGRATION_NAME = "dsl-constants"
        private const val DSL_CONSTANTS_OUTPUT_DIR = "aws-custom-sdk-build-plugin/src/main/resources/dsl-constants"
        private const val ENV_VAR_NAME = "AWS_SDK_KOTLIN_GENERATE_DSL_CONSTANTS"
    }
    
    override val order: Byte = 127 // Run last to ensure all other processing is complete
    
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        // Only generate constants if the environment variable is set to "true"
        // This allows us to control when constants are generated during the build process
        return System.getenv(ENV_VAR_NAME) == "true" ||
               System.getProperty("aws.sdk.kotlin.generate.dsl.constants") == "true"
    }
    
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceName = getServiceName(ctx.settings.sdkId)
        
        // Get all operations for this service
        val operations = TopDownIndex
            .of(ctx.model)
            .getContainedOperations(service)
            .sortedBy { it.id.name }
        
        if (operations.isEmpty()) {
            return // No operations to generate constants for
        }
        
        // Generate the constants file
        generateOperationConstants(ctx, delegator, serviceName, operations)
    }
    
    /**
     * Generate the service name using the same logic as service client generation.
     * This ensures consistency between client names (e.g., "LambdaClient") and 
     * constants names (e.g., "LambdaOperations").
     */
    private fun getServiceName(sdkId: String): String {
        return sdkId.sanitizeClientName().toPascalCase()
    }
    
    /**
     * Sanitize the service name by removing common suffixes, following the same
     * logic as the service client naming in KotlinSymbolProvider.
     */
    private fun String.sanitizeClientName(): String =
        replace(Regex("(API|Client|Service)$", setOf(RegexOption.IGNORE_CASE)), "")
    
    /**
     * Generate operation constants for a specific service
     */
    private fun generateOperationConstants(
        ctx: CodegenContext,
        delegator: KotlinDelegator,
        serviceName: String,
        operations: List<OperationShape>
    ) {
        val className = "${serviceName}Operations"
        val fileName = "$className.kt"
        
        // Generate the file content
        val fileContent = generateFileContent(serviceName, className, operations)
        
        // Write to the normal SDK location
        delegator.useFileWriter(fileName, "aws.sdk.kotlin.gradle.customsdk.constants") { writer ->
            writer.write(fileContent)
        }
        
        // Also write directly to the plugin's resources directory
        writeToPluginResources(fileName, serviceName, fileContent)
    }
    
    /**
     * Generate the content for the DSL constants file
     */
    private fun generateFileContent(serviceName: String, className: String, operations: List<OperationShape>): String {
        val content = StringBuilder()
        
        content.append(
            """
            /*
             * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
             * SPDX-License-Identifier: Apache-2.0
             */
            
            package aws.sdk.kotlin.gradle.customsdk.constants
            
            /**
             * Type-safe operation constants for $serviceName service.
             * 
             * These constants can be used in the AWS Custom SDK Build plugin DSL
             * to specify which operations to include in custom client builds.
             * 
             * Generated during SDK build process.
             */
            object $className {
            """.trimIndent()
        )
        
        // Generate a constant for each operation
        operations.forEach { operation ->
            val operationName = operation.id.name
            content.append(
                """
                
                    /**
                     * Operation: $operationName
                     */
                    const val $operationName = "$operationName"
                """.trimIndent()
            )
        }
        
        content.append("\n}")
        return content.toString()
    }
    
    /**
     * Write the generated file content directly to the plugin's resources directory
     */
    private fun writeToPluginResources(fileName: String, serviceName: String, content: String) {
        try {
            // Determine the output directory relative to the project root
            val projectRoot = findProjectRoot()
            val outputDir = File(projectRoot, DSL_CONSTANTS_OUTPUT_DIR)
            outputDir.mkdirs()
            
            // Write the file to the plugin resources
            val outputFile = File(outputDir, fileName)
            outputFile.writeText(content)
            
            println("Generated DSL constants for $serviceName: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            // Log the error but don't fail the build
            println("Warning: Could not write DSL constants for $serviceName: ${e.message}")
        }
    }
    
    /**
     * Find the project root directory by looking for settings.gradle.kts
     */
    private fun findProjectRoot(): File {
        var current = File(System.getProperty("user.dir"))
        
        while (current.parent != null) {
            if (File(current, "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        
        // Fallback to current directory
        return File(System.getProperty("user.dir"))
    }
}
