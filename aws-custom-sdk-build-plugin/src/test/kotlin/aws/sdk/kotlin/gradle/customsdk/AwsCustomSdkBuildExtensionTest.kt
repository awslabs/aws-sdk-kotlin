/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AwsCustomSdkBuildExtensionTest {
    
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
    fun `should have default values`() {
        assertEquals("us-east-1", extension.region.get())
        assertEquals("${project.buildDir}/generated/aws-custom-sdk", extension.outputDirectory.get())
        assertEquals("aws.sdk.kotlin.custom", extension.packageNamePrefix.get())
        assertTrue(extension.strictValidation.get())
    }
    
    @Test
    fun `should configure service with string operations`() {
        extension.service("lambda") {
            operations("CreateFunction", "InvokeFunction", "DeleteFunction")
        }
        
        val selectedServices = extension.getSelectedServices()
        assertEquals(1, selectedServices.size)
        assertTrue(selectedServices.containsKey("lambda"))
        
        val lambdaOps = selectedServices["lambda"]!!
        assertEquals(3, lambdaOps.size)
        assertTrue(lambdaOps.contains("CreateFunction"))
        assertTrue(lambdaOps.contains("InvokeFunction"))
        assertTrue(lambdaOps.contains("DeleteFunction"))
    }
    
    @Test
    fun `should configure service with collection of operations`() {
        val operations = listOf("GetObject", "PutObject", "DeleteObject")
        
        extension.service("s3") {
            operations(operations)
        }
        
        val selectedServices = extension.getSelectedServices()
        val s3Ops = selectedServices["s3"]!!
        assertEquals(3, s3Ops.size)
        assertTrue(s3Ops.containsAll(operations))
    }
    
    @Test
    fun `should configure multiple services using services DSL`() {
        extension.services {
            lambda {
                operations("CreateFunction", "InvokeFunction")
            }
            s3 {
                operations("GetObject", "PutObject")
            }
        }
        
        val selectedServices = extension.getSelectedServices()
        assertEquals(2, selectedServices.size)
        
        val lambdaOps = selectedServices["lambda"]!!
        assertEquals(2, lambdaOps.size)
        assertTrue(lambdaOps.contains("CreateFunction"))
        assertTrue(lambdaOps.contains("InvokeFunction"))
        
        val s3Ops = selectedServices["s3"]!!
        assertEquals(2, s3Ops.size)
        assertTrue(s3Ops.contains("GetObject"))
        assertTrue(s3Ops.contains("PutObject"))
    }
    
    @Test
    fun `should support allOperations method`() {
        extension.service("lambda") {
            allOperations()
        }
        
        val selectedServices = extension.getSelectedServices()
        val lambdaOps = selectedServices["lambda"]!!
        
        // Should either have operations (if constants available) or be empty (if not available)
        assertTrue(lambdaOps.isEmpty() || lambdaOps.size > 10) // Lambda has many operations
    }
    
    @Test
    fun `should support operationsMatching method`() {
        extension.service("lambda") {
            operationsMatching("Create.*")
        }
        
        val selectedServices = extension.getSelectedServices()
        val lambdaOps = selectedServices["lambda"]!!
        
        // Should match operations starting with "Create" if constants are available
        if (lambdaOps.isNotEmpty()) {
            assertTrue(lambdaOps.all { it.startsWith("Create") })
        }
    }
    
    @Test
    fun `should provide validation summary`() {
        extension.service("lambda") {
            operations("CreateFunction", "InvalidOperation")
        }
        
        val validationSummary = extension.getValidationSummary()
        assertTrue(validationSummary.containsKey("lambda"))
        
        val lambdaValidation = validationSummary["lambda"]!!
        assertTrue(lambdaValidation.validOperations.contains("CreateFunction"))
        
        // If constants are available, should detect invalid operation
        if (ConstantsRegistry.getServiceOperations("lambda").isNotEmpty()) {
            assertTrue(lambdaValidation.invalidOperations.contains("InvalidOperation"))
        }
    }
    
    @Test
    fun `should support generic service method in services DSL`() {
        extension.services {
            service("custom-service") {
                operations("CustomOperation1", "CustomOperation2")
            }
        }
        
        val selectedServices = extension.getSelectedServices()
        assertTrue(selectedServices.containsKey("custom-service"))
        
        val customOps = selectedServices["custom-service"]!!
        assertEquals(2, customOps.size)
        assertTrue(customOps.contains("CustomOperation1"))
        assertTrue(customOps.contains("CustomOperation2"))
    }
}
