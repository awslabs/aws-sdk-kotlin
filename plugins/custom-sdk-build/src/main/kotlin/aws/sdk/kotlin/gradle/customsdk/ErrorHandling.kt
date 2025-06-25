/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.io.IOException

/**
 * Comprehensive error handling utilities for custom SDK build operations.
 * Provides clear, actionable error messages and recovery suggestions.
 */
object ErrorHandling {
    
    /**
     * Handle model loading errors with detailed diagnostics.
     */
    fun handleModelLoadingError(modelsDirectory: File, cause: Throwable, logger: Logger): Nothing {
        val message = buildString {
            appendLine("Failed to load AWS service models")
            appendLine()
            appendLine("Attempted to load models from: ${modelsDirectory.absolutePath}")
            appendLine()
            
            when {
                !modelsDirectory.exists() -> {
                    appendLine("❌ Models directory does not exist")
                    appendLine("💡 Ensure the AWS SDK models are available in your build")
                    appendLine("   This usually indicates a build configuration issue")
                }
                !modelsDirectory.isDirectory -> {
                    appendLine("❌ Models path is not a directory")
                    appendLine("💡 Check that the models path points to a directory containing .json files")
                }
                modelsDirectory.listFiles()?.isEmpty() == true -> {
                    appendLine("❌ Models directory is empty")
                    appendLine("💡 Ensure AWS service model files (.json) are present in the directory")
                }
                else -> {
                    val modelFiles = modelsDirectory.listFiles { file -> file.extension == "json" }
                    appendLine("📁 Directory contents:")
                    modelsDirectory.listFiles()?.take(10)?.forEach { file ->
                        appendLine("   - ${file.name} (${if (file.isDirectory) "directory" else "${file.length()} bytes"})")
                    }
                    if (modelFiles?.isEmpty() == true) {
                        appendLine("❌ No .json model files found")
                        appendLine("💡 AWS service models should be .json files")
                    }
                }
            }
            
            appendLine()
            appendLine("🔧 Troubleshooting steps:")
            appendLine("1. Verify the AWS SDK for Kotlin is properly configured in your build")
            appendLine("2. Check that model files are included in your build dependencies")
            appendLine("3. Ensure the models directory path is correct")
            appendLine("4. Try running 'gradle clean' and rebuilding")
            
            if (cause.message != null) {
                appendLine()
                appendLine("Underlying error: ${cause.message}")
            }
        }
        
        logger.error(message)
        throw GradleException("Model loading failed", cause)
    }
    
    /**
     * Handle Smithy build execution errors.
     */
    fun handleSmithyBuildError(projectionFile: File, cause: Throwable, logger: Logger): Nothing {
        val message = buildString {
            appendLine("Smithy build execution failed")
            appendLine()
            appendLine("Projection file: ${projectionFile.absolutePath}")
            appendLine()
            
            when (cause) {
                is IOException -> {
                    appendLine("❌ I/O error during Smithy build")
                    appendLine("💡 Check file permissions and disk space")
                    appendLine("   Ensure the output directory is writable")
                }
                is IllegalArgumentException -> {
                    appendLine("❌ Invalid Smithy build configuration")
                    appendLine("💡 The generated projection configuration may be invalid")
                    appendLine("   This could indicate a bug in the plugin")
                }
                else -> {
                    appendLine("❌ Unexpected error during Smithy build")
                    appendLine("💡 This may indicate an issue with the Smithy build process")
                }
            }
            
            // Show projection file contents if it exists and is readable
            if (projectionFile.exists() && projectionFile.canRead()) {
                try {
                    val content = projectionFile.readText()
                    if (content.length < 2000) { // Only show if reasonably sized
                        appendLine()
                        appendLine("📄 Projection configuration:")
                        appendLine(content)
                    }
                } catch (e: Exception) {
                    appendLine("   (Could not read projection file: ${e.message})")
                }
            }
            
            appendLine()
            appendLine("🔧 Troubleshooting steps:")
            appendLine("1. Check that all selected operations exist in the service models")
            appendLine("2. Verify that the Smithy build tools are properly configured")
            appendLine("3. Try with a smaller set of operations to isolate the issue")
            appendLine("4. Check the Gradle build logs for more detailed error information")
            
            if (cause.message != null) {
                appendLine()
                appendLine("Underlying error: ${cause.message}")
            }
        }
        
        logger.error(message)
        throw GradleException("Smithy build failed", cause)
    }
    
    /**
     * Handle code generation errors.
     */
    fun handleCodeGenerationError(outputDirectory: File, cause: Throwable, logger: Logger): Nothing {
        val message = buildString {
            appendLine("Code generation failed")
            appendLine()
            appendLine("Output directory: ${outputDirectory.absolutePath}")
            appendLine()
            
            when (cause) {
                is IOException -> {
                    appendLine("❌ I/O error during code generation")
                    appendLine("💡 Check file permissions and disk space")
                    if (!outputDirectory.exists()) {
                        appendLine("   Output directory does not exist")
                    } else if (!outputDirectory.canWrite()) {
                        appendLine("   Output directory is not writable")
                    }
                }
                is SecurityException -> {
                    appendLine("❌ Security error during code generation")
                    appendLine("💡 Check file system permissions")
                    appendLine("   The build process may not have permission to write to the output directory")
                }
                else -> {
                    appendLine("❌ Unexpected error during code generation")
                    appendLine("💡 This may indicate an issue with the code generation process")
                }
            }
            
            appendLine()
            appendLine("🔧 Troubleshooting steps:")
            appendLine("1. Ensure the output directory is writable")
            appendLine("2. Check available disk space")
            appendLine("3. Try cleaning the build directory: gradle clean")
            appendLine("4. Verify file system permissions")
            
            if (cause.message != null) {
                appendLine()
                appendLine("Underlying error: ${cause.message}")
            }
        }
        
        logger.error(message)
        throw GradleException("Code generation failed", cause)
    }
    
    /**
     * Handle dependency resolution errors.
     */
    fun handleDependencyResolutionError(cause: Throwable, logger: Logger): Nothing {
        val message = buildString {
            appendLine("Dependency resolution failed")
            appendLine()
            
            when {
                cause.message?.contains("Could not resolve") == true -> {
                    appendLine("❌ Could not resolve required dependencies")
                    appendLine("💡 Check your repository configuration and network connectivity")
                    appendLine("   Ensure Maven Central and other required repositories are accessible")
                }
                cause.message?.contains("version") == true -> {
                    appendLine("❌ Version conflict or missing version")
                    appendLine("💡 Check that all dependencies have compatible versions")
                    appendLine("   The custom SDK plugin version should match your AWS SDK version")
                }
                else -> {
                    appendLine("❌ Unexpected dependency resolution error")
                    appendLine("💡 This may indicate a configuration issue")
                }
            }
            
            appendLine()
            appendLine("🔧 Troubleshooting steps:")
            appendLine("1. Check your repository configuration in build.gradle")
            appendLine("2. Verify network connectivity to Maven repositories")
            appendLine("3. Try refreshing dependencies: gradle --refresh-dependencies")
            appendLine("4. Check for version conflicts in your dependency tree")
            
            if (cause.message != null) {
                appendLine()
                appendLine("Underlying error: ${cause.message}")
            }
        }
        
        logger.error(message)
        throw GradleException("Dependency resolution failed", cause)
    }
    
    /**
     * Handle task execution errors with context.
     */
    fun handleTaskExecutionError(taskName: String, cause: Throwable, logger: Logger): Nothing {
        val message = buildString {
            appendLine("Task '$taskName' execution failed")
            appendLine()
            
            when (cause) {
                is IllegalStateException -> {
                    appendLine("❌ Invalid task state")
                    appendLine("💡 This usually indicates a configuration problem")
                    appendLine("   Check that the plugin is properly configured")
                }
                is IllegalArgumentException -> {
                    appendLine("❌ Invalid task arguments")
                    appendLine("💡 Check the task configuration parameters")
                    appendLine("   Ensure all required properties are set correctly")
                }
                is IOException -> {
                    appendLine("❌ I/O error during task execution")
                    appendLine("💡 Check file permissions and disk space")
                }
                else -> {
                    appendLine("❌ Unexpected task execution error")
                    appendLine("💡 Check the Gradle build logs for more details")
                }
            }
            
            appendLine()
            appendLine("🔧 Troubleshooting steps:")
            appendLine("1. Check the plugin configuration in your build.gradle")
            appendLine("2. Verify that all required dependencies are available")
            appendLine("3. Try running with --stacktrace for more detailed error information")
            appendLine("4. Check the Gradle daemon logs for additional context")
            
            if (cause.message != null) {
                appendLine()
                appendLine("Underlying error: ${cause.message}")
            }
        }
        
        logger.error(message)
        throw GradleException("Task execution failed", cause)
    }
    
    /**
     * Provide recovery suggestions for common error scenarios.
     */
    fun suggestRecoveryActions(error: Throwable, logger: Logger) {
        logger.info("🔧 Recovery suggestions:")
        
        when {
            error.message?.contains("OutOfMemoryError") == true -> {
                logger.info("• Increase JVM heap size: gradle -Xmx2g")
                logger.info("• Reduce the number of operations in your custom SDK")
                logger.info("• Split large configurations into multiple smaller SDKs")
            }
            error.message?.contains("PermissionDenied") == true -> {
                logger.info("• Check file system permissions")
                logger.info("• Ensure the build directory is writable")
                logger.info("• Try running as administrator/root if necessary")
            }
            error.message?.contains("NoSuchFile") == true -> {
                logger.info("• Verify all file paths in your configuration")
                logger.info("• Check that required model files are present")
                logger.info("• Try cleaning and rebuilding: gradle clean build")
            }
            error.message?.contains("timeout") == true -> {
                logger.info("• Increase build timeout settings")
                logger.info("• Check network connectivity")
                logger.info("• Try building with fewer parallel workers")
            }
            else -> {
                logger.info("• Try cleaning the build: gradle clean")
                logger.info("• Check the full error logs with --stacktrace")
                logger.info("• Verify your plugin and SDK versions are compatible")
            }
        }
    }
    
    /**
     * Create a user-friendly error summary.
     */
    fun createErrorSummary(error: Throwable): String {
        return buildString {
            appendLine("❌ Custom SDK Build Failed")
            appendLine()
            appendLine("Error: ${error.message ?: error.javaClass.simpleName}")
            appendLine()
            appendLine("This error occurred during custom SDK generation.")
            appendLine("Check the detailed error message above for specific troubleshooting steps.")
            appendLine()
            appendLine("For additional help:")
            appendLine("• Check the AWS SDK for Kotlin documentation")
            appendLine("• Review your plugin configuration")
            appendLine("• Try with a simpler configuration to isolate the issue")
        }
    }
}
