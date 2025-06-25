/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationEngineTest {
    
    @Test
    fun `validation passes for valid configuration`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject", "com.amazonaws.s3#PutObject"),
            "dynamodb" to listOf("com.amazonaws.dynamodb#GetItem")
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `validation fails for empty configuration`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = emptyMap<String, List<String>>()
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "NO_SERVICES_SELECTED" })
    }
    
    @Test
    fun `validation fails for services with no operations`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to emptyList<String>()
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "NO_OPERATIONS_SELECTED" })
    }
    
    @Test
    fun `validation warns about large configurations`() {
        val project = ProjectBuilder.builder().build()
        val largeOperationList = (1..250).map { "com.amazonaws.s3#Operation$it" }
        val selectedOperations = mapOf("s3" to largeOperationList)
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertTrue(result.isValid) // Should still be valid
        assertTrue(result.warnings.any { it.code == "LARGE_SDK_CONFIGURATION" })
    }
    
    @Test
    fun `validation fails for invalid service names`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "Invalid-Service-Name!" to listOf("com.amazonaws.s3#GetObject")
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "INVALID_SERVICE_NAME" })
    }
    
    @Test
    fun `validation fails for invalid operation shape IDs`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("invalid-shape-id")
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "INVALID_OPERATION_SHAPE_ID" })
    }
    
    @Test
    fun `validation fails for operation service mismatch`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.dynamodb#GetItem") // DynamoDB operation in S3 service
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "OPERATION_SERVICE_MISMATCH" })
    }
    
    @Test
    fun `validation fails for invalid package name`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject")
        )
        val packageName = "Invalid.Package.Name!" // Invalid characters
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "INVALID_PACKAGE_NAME" })
    }
    
    @Test
    fun `validation fails for invalid package version`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject")
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "invalid-version" // Invalid format
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.code == "INVALID_PACKAGE_VERSION" })
    }
    
    @Test
    fun `validation warns about non-standard package names`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject")
        )
        val packageName = "com.example.custom.sdk" // Valid but non-standard
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "NON_STANDARD_PACKAGE_NAME" })
    }
    
    @Test
    fun `validation warns about single operation services`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf("com.amazonaws.s3#GetObject") // Only one operation
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "SINGLE_OPERATION_SERVICE" })
    }
    
    @Test
    fun `validation warns about duplicate operations`() {
        val project = ProjectBuilder.builder().build()
        val selectedOperations = mapOf(
            "s3" to listOf(
                "com.amazonaws.s3#GetObject",
                "com.amazonaws.s3#GetObject" // Duplicate
            )
        )
        val packageName = "aws.sdk.kotlin.services.custom"
        val packageVersion = "1.0.0"
        
        val result = ValidationEngine.validateConfiguration(
            project, selectedOperations, packageName, packageVersion
        )
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.code == "DUPLICATE_OPERATION" })
    }
    
    @Test
    fun `service name validation works correctly`() {
        // Valid service names
        assertTrue(ValidationEngine.isValidServiceName("s3"))
        assertTrue(ValidationEngine.isValidServiceName("dynamodb"))
        assertTrue(ValidationEngine.isValidServiceName("lambda"))
        assertTrue(ValidationEngine.isValidServiceName("ec2"))
        assertTrue(ValidationEngine.isValidServiceName("api-gateway"))
        
        // Invalid service names
        assertFalse(ValidationEngine.isValidServiceName("S3")) // Uppercase
        assertFalse(ValidationEngine.isValidServiceName("s3!")) // Special characters
        assertFalse(ValidationEngine.isValidServiceName("")) // Empty
        assertFalse(ValidationEngine.isValidServiceName("-s3")) // Starts with hyphen
        assertFalse(ValidationEngine.isValidServiceName("s3-")) // Ends with hyphen
    }
    
    @Test
    fun `operation shape ID validation works correctly`() {
        // Valid shape IDs
        assertTrue(ValidationEngine.isValidOperationShapeId("com.amazonaws.s3#GetObject"))
        assertTrue(ValidationEngine.isValidOperationShapeId("com.amazonaws.dynamodb#GetItem"))
        assertTrue(ValidationEngine.isValidOperationShapeId("com.example.service#MyOperation"))
        
        // Invalid shape IDs
        assertFalse(ValidationEngine.isValidOperationShapeId("GetObject")) // No namespace
        assertFalse(ValidationEngine.isValidOperationShapeId("com.amazonaws.s3")) // No operation
        assertFalse(ValidationEngine.isValidOperationShapeId("com.amazonaws.s3#getObject")) // Lowercase operation
        assertFalse(ValidationEngine.isValidOperationShapeId("")) // Empty
    }
    
    @Test
    fun `package name validation works correctly`() {
        // Valid package names
        assertTrue(ValidationEngine.isValidPackageName("aws.sdk.kotlin"))
        assertTrue(ValidationEngine.isValidPackageName("com.example.package"))
        assertTrue(ValidationEngine.isValidPackageName("a"))
        
        // Invalid package names
        assertFalse(ValidationEngine.isValidPackageName("Aws.sdk.kotlin")) // Uppercase
        assertFalse(ValidationEngine.isValidPackageName("aws.sdk.kotlin!")) // Special characters
        assertFalse(ValidationEngine.isValidPackageName("")) // Empty
        assertFalse(ValidationEngine.isValidPackageName(".aws.sdk")) // Starts with dot
        assertFalse(ValidationEngine.isValidPackageName("aws.sdk.")) // Ends with dot
    }
    
    @Test
    fun `package version validation works correctly`() {
        // Valid versions
        assertTrue(ValidationEngine.isValidPackageVersion("1.0.0"))
        assertTrue(ValidationEngine.isValidPackageVersion("1.2.3"))
        assertTrue(ValidationEngine.isValidPackageVersion("1.0.0-SNAPSHOT"))
        assertTrue(ValidationEngine.isValidPackageVersion("2.1.0-beta.1"))
        
        // Invalid versions
        assertFalse(ValidationEngine.isValidPackageVersion("1.0")) // Missing patch version
        assertFalse(ValidationEngine.isValidPackageVersion("v1.0.0")) // Prefix
        assertFalse(ValidationEngine.isValidPackageVersion("")) // Empty
        assertFalse(ValidationEngine.isValidPackageVersion("1.0.0.0")) // Too many parts
    }
}
