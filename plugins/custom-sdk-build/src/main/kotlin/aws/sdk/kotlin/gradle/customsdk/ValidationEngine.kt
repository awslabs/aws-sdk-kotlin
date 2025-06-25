/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Comprehensive validation engine for custom SDK build configuration.
 * Provides clear, actionable error messages to help users resolve configuration issues.
 */
object ValidationEngine {
    
    /**
     * Validate the complete custom SDK configuration.
     */
    fun validateConfiguration(
        project: Project,
        selectedOperations: Map<String, List<String>>,
        packageName: String,
        packageVersion: String
    ): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        val warnings = mutableListOf<ValidationWarning>()
        
        // Validate basic configuration
        validateBasicConfiguration(selectedOperations, packageName, packageVersion, errors, warnings)
        
        // Validate service selections
        validateServiceSelections(selectedOperations, errors, warnings)
        
        // Validate operation selections
        validateOperationSelections(selectedOperations, errors, warnings)
        
        // Validate package configuration
        validatePackageConfiguration(packageName, packageVersion, errors, warnings)
        
        // Validate project environment
        validateProjectEnvironment(project, errors, warnings)
        
        return ValidationResult(errors, warnings)
    }
    
    /**
     * Validate basic configuration requirements.
     */
    private fun validateBasicConfiguration(
        selectedOperations: Map<String, List<String>>,
        packageName: String,
        packageVersion: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // Check if any operations are selected
        if (selectedOperations.isEmpty()) {
            errors.add(ValidationError(
                code = "NO_SERVICES_SELECTED",
                message = "No services configured for custom SDK generation",
                suggestion = "Add at least one service configuration using the DSL:\n" +
                        "awsCustomSdkBuild {\n" +
                        "    s3 {\n" +
                        "        operations(S3Operation.GetObject, S3Operation.PutObject)\n" +
                        "    }\n" +
                        "}"
            ))
        }
        
        // Check if any operations are selected within services
        val totalOperations = selectedOperations.values.sumOf { it.size }
        if (totalOperations == 0) {
            errors.add(ValidationError(
                code = "NO_OPERATIONS_SELECTED",
                message = "No operations selected for custom SDK generation",
                suggestion = "Add operations to your service configurations:\n" +
                        "s3 {\n" +
                        "    operations(S3Operation.GetObject, S3Operation.PutObject)\n" +
                        "}"
            ))
        }
        
        // Warn about very large SDK configurations
        if (totalOperations > 200) {
            warnings.add(ValidationWarning(
                code = "LARGE_SDK_CONFIGURATION",
                message = "Large custom SDK configuration detected ($totalOperations operations)",
                suggestion = "Consider splitting into multiple smaller custom SDKs for better build performance"
            ))
        }
    }
    
    /**
     * Validate service selections.
     */
    private fun validateServiceSelections(
        selectedOperations: Map<String, List<String>>,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        selectedOperations.forEach { (serviceName, operations) ->
            // Validate service name format
            if (!isValidServiceName(serviceName)) {
                errors.add(ValidationError(
                    code = "INVALID_SERVICE_NAME",
                    message = "Invalid service name: '$serviceName'",
                    suggestion = "Service names must be lowercase alphanumeric with optional hyphens. " +
                            "Use generated service methods like s3 { }, dynamodb { }, lambda { }"
                ))
            }
            
            // Check for empty operation lists
            if (operations.isEmpty()) {
                warnings.add(ValidationWarning(
                    code = "EMPTY_SERVICE_CONFIGURATION",
                    message = "Service '$serviceName' has no operations selected",
                    suggestion = "Add operations to the service configuration or remove the empty service block"
                ))
            }
            
            // Warn about single-operation services
            if (operations.size == 1) {
                warnings.add(ValidationWarning(
                    code = "SINGLE_OPERATION_SERVICE",
                    message = "Service '$serviceName' has only one operation selected",
                    suggestion = "Consider if using the full pre-built SDK client might be more appropriate"
                ))
            }
        }
    }
    
    /**
     * Validate operation selections.
     */
    private fun validateOperationSelections(
        selectedOperations: Map<String, List<String>>,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        selectedOperations.forEach { (serviceName, operations) ->
            operations.forEach { operationShapeId ->
                // Validate operation shape ID format
                if (!isValidOperationShapeId(operationShapeId)) {
                    errors.add(ValidationError(
                        code = "INVALID_OPERATION_SHAPE_ID",
                        message = "Invalid operation shape ID: '$operationShapeId' in service '$serviceName'",
                        suggestion = "Operation shape IDs must be in format 'namespace#OperationName'. " +
                                "Use typed operation constants like S3Operation.GetObject instead of strings"
                    ))
                }
                
                // Validate operation belongs to service
                if (!operationBelongsToService(operationShapeId, serviceName)) {
                    errors.add(ValidationError(
                        code = "OPERATION_SERVICE_MISMATCH",
                        message = "Operation '$operationShapeId' does not belong to service '$serviceName'",
                        suggestion = "Ensure operations are configured under the correct service block"
                    ))
                }
            }
            
            // Check for duplicate operations
            val duplicates = operations.groupingBy { it }.eachCount().filter { it.value > 1 }
            duplicates.forEach { (operation, count) ->
                warnings.add(ValidationWarning(
                    code = "DUPLICATE_OPERATION",
                    message = "Operation '$operation' is specified $count times in service '$serviceName'",
                    suggestion = "Remove duplicate operation specifications"
                ))
            }
        }
    }
    
    /**
     * Validate package configuration.
     */
    private fun validatePackageConfiguration(
        packageName: String,
        packageVersion: String,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // Validate package name format
        if (!isValidPackageName(packageName)) {
            errors.add(ValidationError(
                code = "INVALID_PACKAGE_NAME",
                message = "Invalid package name: '$packageName'",
                suggestion = "Package names must follow Java package naming conventions (lowercase, dot-separated)"
            ))
        }
        
        // Validate package version format
        if (!isValidPackageVersion(packageVersion)) {
            errors.add(ValidationError(
                code = "INVALID_PACKAGE_VERSION",
                message = "Invalid package version: '$packageVersion'",
                suggestion = "Package versions should follow semantic versioning (e.g., '1.0.0')"
            ))
        }
        
        // Warn about non-standard package names
        if (!packageName.startsWith("aws.sdk.kotlin")) {
            warnings.add(ValidationWarning(
                code = "NON_STANDARD_PACKAGE_NAME",
                message = "Package name '$packageName' doesn't follow AWS SDK conventions",
                suggestion = "Consider using a package name starting with 'aws.sdk.kotlin' for consistency"
            ))
        }
    }
    
    /**
     * Validate project environment.
     */
    private fun validateProjectEnvironment(
        project: Project,
        errors: MutableList<ValidationError>,
        warnings: MutableList<ValidationWarning>
    ) {
        // Check for Kotlin plugin
        val hasKotlinPlugin = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") ||
                project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
        
        if (!hasKotlinPlugin) {
            warnings.add(ValidationWarning(
                code = "NO_KOTLIN_PLUGIN",
                message = "No Kotlin plugin detected in project",
                suggestion = "Apply the Kotlin JVM or Multiplatform plugin for optimal integration:\n" +
                        "plugins {\n" +
                        "    kotlin(\"jvm\")\n" +
                        "    // or kotlin(\"multiplatform\")\n" +
                        "}"
            ))
        }
        
        // Check Java version
        val javaVersion = System.getProperty("java.version")
        if (javaVersion.startsWith("1.8") || javaVersion.startsWith("8")) {
            warnings.add(ValidationWarning(
                code = "OLD_JAVA_VERSION",
                message = "Java 8 detected. AWS SDK for Kotlin requires Java 11+",
                suggestion = "Upgrade to Java 11 or later for full compatibility"
            ))
        }
        
        // Check for build cache configuration
        if (!project.gradle.startParameter.isBuildCacheEnabled) {
            warnings.add(ValidationWarning(
                code = "BUILD_CACHE_DISABLED",
                message = "Gradle build cache is disabled",
                suggestion = "Enable build cache for better performance:\n" +
                        "gradle.properties: org.gradle.caching=true\n" +
                        "Or use --build-cache flag"
            ))
        }
    }
    
    /**
     * Validate service name format.
     */
    fun isValidServiceName(serviceName: String): Boolean {
        return serviceName.matches(Regex("^[a-z][a-z0-9-]*[a-z0-9]$")) || serviceName.matches(Regex("^[a-z]$"))
    }
    
    /**
     * Validate operation shape ID format.
     */
    fun isValidOperationShapeId(shapeId: String): Boolean {
        return shapeId.matches(Regex("^[a-zA-Z][a-zA-Z0-9._-]*#[A-Z][a-zA-Z0-9]*$"))
    }
    
    /**
     * Check if operation belongs to the specified service.
     */
    private fun operationBelongsToService(operationShapeId: String, serviceName: String): Boolean {
        // Extract namespace from shape ID
        val namespace = operationShapeId.substringBefore("#")
        
        // Map service names to expected namespaces
        val expectedNamespace = when (serviceName.lowercase()) {
            "s3" -> "com.amazonaws.s3"
            "dynamodb" -> "com.amazonaws.dynamodb"
            "lambda" -> "com.amazonaws.lambda"
            "ec2" -> "com.amazonaws.ec2"
            "iam" -> "com.amazonaws.iam"
            "sns" -> "com.amazonaws.sns"
            "sqs" -> "com.amazonaws.sqs"
            "rds" -> "com.amazonaws.rds"
            "cloudformation" -> "com.amazonaws.cloudformation"
            "cloudwatch" -> "com.amazonaws.cloudwatch"
            else -> "com.amazonaws.$serviceName"
        }
        
        return namespace == expectedNamespace
    }
    
    /**
     * Validate package name format.
     */
    fun isValidPackageName(packageName: String): Boolean {
        return packageName.matches(Regex("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)*$"))
    }
    
    /**
     * Validate package version format.
     */
    fun isValidPackageVersion(version: String): Boolean {
        return version.matches(Regex("^\\d+\\.\\d+\\.\\d+(?:-[a-zA-Z0-9.-]+)?$"))
    }
    
    /**
     * Log validation results with appropriate severity levels.
     */
    fun logValidationResults(logger: Logger, result: ValidationResult) {
        if (result.errors.isNotEmpty()) {
            logger.error("Custom SDK Build Configuration Errors:")
            result.errors.forEach { error ->
                logger.error("  [${error.code}] ${error.message}")
                if (error.suggestion.isNotEmpty()) {
                    logger.error("    Suggestion: ${error.suggestion}")
                }
            }
        }
        
        if (result.warnings.isNotEmpty()) {
            logger.warn("Custom SDK Build Configuration Warnings:")
            result.warnings.forEach { warning ->
                logger.warn("  [${warning.code}] ${warning.message}")
                if (warning.suggestion.isNotEmpty()) {
                    logger.warn("    Suggestion: ${warning.suggestion}")
                }
            }
        }
        
        if (result.isValid) {
            logger.info("Custom SDK configuration validation passed")
        }
    }
    
    /**
     * Throw a comprehensive exception for validation failures.
     */
    fun throwValidationException(result: ValidationResult) {
        if (!result.isValid) {
            val message = buildString {
                appendLine("Custom SDK Build configuration validation failed:")
                appendLine()
                
                result.errors.forEach { error ->
                    appendLine("❌ [${error.code}] ${error.message}")
                    if (error.suggestion.isNotEmpty()) {
                        appendLine("   💡 ${error.suggestion}")
                    }
                    appendLine()
                }
                
                if (result.warnings.isNotEmpty()) {
                    appendLine("Warnings:")
                    result.warnings.forEach { warning ->
                        appendLine("⚠️  [${warning.code}] ${warning.message}")
                        if (warning.suggestion.isNotEmpty()) {
                            appendLine("   💡 ${warning.suggestion}")
                        }
                        appendLine()
                    }
                }
                
                appendLine("Please fix the configuration errors and try again.")
            }
            
            throw GradleException(message)
        }
    }
}

/**
 * Result of configuration validation.
 */
data class ValidationResult(
    val errors: List<ValidationError>,
    val warnings: List<ValidationWarning>
) {
    val isValid: Boolean get() = errors.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

/**
 * Validation error with code and suggestion.
 */
data class ValidationError(
    val code: String,
    val message: String,
    val suggestion: String = ""
)

/**
 * Validation warning with code and suggestion.
 */
data class ValidationWarning(
    val code: String,
    val message: String,
    val suggestion: String = ""
)
