/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import aws.sdk.kotlin.gradle.customsdk.constants.LambdaOperations
import aws.sdk.kotlin.gradle.customsdk.constants.S3Operations
import aws.sdk.kotlin.gradle.customsdk.constants.DynamoDbOperations
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Demonstrates the type-safe DSL functionality with real operation constants.
 * This test shows how developers can use the generated constants for type-safe configuration.
 */
class TypeSafeDslExampleTest {
    
    private lateinit var project: Project
    private lateinit var extension: AwsCustomSdkBuildExtension
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
        extension = project.extensions.create(
            "awsCustomSdk",
            AwsCustomSdkBuildExtension::class.java,
            project
        )
    }
    
    @Test
    fun `should support type-safe Lambda operations`() {
        extension.service("lambda") {
            operations(
                LambdaOperations.CreateFunction,
                LambdaOperations.Invoke,
                LambdaOperations.DeleteFunction,
                LambdaOperations.UpdateFunctionCode
            )
        }
        
        val selectedServices = extension.getSelectedServices()
        val lambdaOps = selectedServices["lambda"]!!
        
        assertEquals(4, lambdaOps.size)
        assertTrue(lambdaOps.contains("CreateFunction"))
        assertTrue(lambdaOps.contains("Invoke"))
        assertTrue(lambdaOps.contains("DeleteFunction"))
        assertTrue(lambdaOps.contains("UpdateFunctionCode"))
    }
    
    @Test
    fun `should support type-safe S3 operations`() {
        extension.service("s3") {
            operations(
                S3Operations.GetObject,
                S3Operations.PutObject,
                S3Operations.DeleteObject,
                S3Operations.ListObjects
            )
        }
        
        val selectedServices = extension.getSelectedServices()
        val s3Ops = selectedServices["s3"]!!
        
        assertEquals(4, s3Ops.size)
        assertTrue(s3Ops.contains("GetObject"))
        assertTrue(s3Ops.contains("PutObject"))
        assertTrue(s3Ops.contains("DeleteObject"))
        assertTrue(s3Ops.contains("ListObjects"))
    }
    
    @Test
    fun `should support type-safe DynamoDB operations`() {
        extension.service("dynamodb") {
            operations(
                DynamoDbOperations.GetItem,
                DynamoDbOperations.PutItem,
                DynamoDbOperations.UpdateItem,
                DynamoDbOperations.DeleteItem,
                DynamoDbOperations.Query,
                DynamoDbOperations.Scan
            )
        }
        
        val selectedServices = extension.getSelectedServices()
        val dynamoOps = selectedServices["dynamodb"]!!
        
        assertEquals(6, dynamoOps.size)
        assertTrue(dynamoOps.contains("GetItem"))
        assertTrue(dynamoOps.contains("PutItem"))
        assertTrue(dynamoOps.contains("UpdateItem"))
        assertTrue(dynamoOps.contains("DeleteItem"))
        assertTrue(dynamoOps.contains("Query"))
        assertTrue(dynamoOps.contains("Scan"))
    }
    
    @Test
    fun `should support mixed type-safe and string operations`() {
        extension.services {
            lambda {
                // Type-safe constants
                operations(
                    LambdaOperations.CreateFunction,
                    LambdaOperations.Invoke
                )
            }
            s3 {
                // String literals (backward compatibility)
                operations("GetObject", "PutObject")
            }
            dynamodb {
                // Collection of type-safe constants
                operations(listOf(
                    DynamoDbOperations.GetItem,
                    DynamoDbOperations.PutItem
                ))
            }
        }
        
        val selectedServices = extension.getSelectedServices()
        assertEquals(3, selectedServices.size)
        
        // Verify Lambda operations
        val lambdaOps = selectedServices["lambda"]!!
        assertEquals(2, lambdaOps.size)
        assertTrue(lambdaOps.contains("CreateFunction"))
        assertTrue(lambdaOps.contains("Invoke"))
        
        // Verify S3 operations
        val s3Ops = selectedServices["s3"]!!
        assertEquals(2, s3Ops.size)
        assertTrue(s3Ops.contains("GetObject"))
        assertTrue(s3Ops.contains("PutObject"))
        
        // Verify DynamoDB operations
        val dynamoOps = selectedServices["dynamodb"]!!
        assertEquals(2, dynamoOps.size)
        assertTrue(dynamoOps.contains("GetItem"))
        assertTrue(dynamoOps.contains("PutItem"))
    }
    
    @Test
    fun `should demonstrate advanced DSL features`() {
        extension.services {
            lambda {
                // Add specific operations
                operations(
                    LambdaOperations.CreateFunction,
                    LambdaOperations.UpdateFunctionCode
                )
                
                // Add operations matching a pattern
                operationsMatching("List.*")
            }
            
            s3 {
                // Add all available operations
                allOperations()
            }
        }
        
        val selectedServices = extension.getSelectedServices()
        
        // Lambda should have specific operations plus List* operations
        val lambdaOps = selectedServices["lambda"]!!
        assertTrue(lambdaOps.contains("CreateFunction"))
        assertTrue(lambdaOps.contains("UpdateFunctionCode"))
        assertTrue(lambdaOps.any { it.startsWith("List") })
        
        // S3 should have all available operations (99 operations)
        val s3Ops = selectedServices["s3"]!!
        assertTrue(s3Ops.size > 90) // S3 has many operations
    }
    
    @Test
    fun `should provide comprehensive validation`() {
        // Disable strict validation for this test
        extension.strictValidation.set(false)
        
        extension.service("lambda") {
            operations(
                LambdaOperations.CreateFunction,  // Valid
                "InvalidOperation"                // Invalid
            )
        }
        
        val validationSummary = extension.getValidationSummary()
        val lambdaValidation = validationSummary["lambda"]!!
        
        assertTrue(lambdaValidation.validOperations.contains("CreateFunction"))
        assertTrue(lambdaValidation.invalidOperations.contains("InvalidOperation"))
        assertTrue(lambdaValidation.message.contains("Invalid operations"))
    }
}
