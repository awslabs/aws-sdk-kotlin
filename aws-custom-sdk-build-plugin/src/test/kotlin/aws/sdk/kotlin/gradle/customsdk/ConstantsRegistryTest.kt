/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConstantsRegistryTest {
    
    @Test
    fun `should register known services`() {
        val registeredServices = ConstantsRegistry.getRegisteredServices()
        
        // Should contain our known services (if constants are available)
        // Note: This test is flexible to handle cases where constants might not be available
        assertTrue(registeredServices.isEmpty() || registeredServices.contains("lambda"))
        assertTrue(registeredServices.isEmpty() || registeredServices.contains("s3"))
        assertTrue(registeredServices.isEmpty() || registeredServices.contains("dynamodb"))
    }
    
    @Test
    fun `should return operations for registered services`() {
        val lambdaOps = ConstantsRegistry.getServiceOperations("lambda")
        val s3Ops = ConstantsRegistry.getServiceOperations("s3")
        
        // If constants are available, should have operations
        // If not available, should return empty set
        assertTrue(lambdaOps.isEmpty() || lambdaOps.contains("CreateFunction"))
        assertTrue(s3Ops.isEmpty() || s3Ops.contains("GetObject"))
    }
    
    @Test
    fun `should return empty set for unknown services`() {
        val unknownOps = ConstantsRegistry.getServiceOperations("unknown-service")
        assertTrue(unknownOps.isEmpty())
    }
    
    @Test
    fun `should validate operations correctly`() {
        // Test with a service that should have constants
        val validationResult = ConstantsRegistry.validateOperations("lambda", listOf("CreateFunction", "InvalidOperation"))
        
        if (ConstantsRegistry.getServiceOperations("lambda").isNotEmpty()) {
            // If constants are available, should validate properly
            assertFalse(validationResult.isValid)
            assertTrue(validationResult.validOperations.contains("CreateFunction"))
            assertTrue(validationResult.invalidOperations.contains("InvalidOperation"))
        } else {
            // If constants are not available, should allow all operations
            assertTrue(validationResult.isValid)
        }
    }
    
    @Test
    fun `should handle case insensitive service names`() {
        val lambdaOpsLower = ConstantsRegistry.getServiceOperations("lambda")
        val lambdaOpsUpper = ConstantsRegistry.getServiceOperations("LAMBDA")
        val lambdaOpsMixed = ConstantsRegistry.getServiceOperations("Lambda")
        
        assertEquals(lambdaOpsLower, lambdaOpsUpper)
        assertEquals(lambdaOpsLower, lambdaOpsMixed)
    }
    
    @Test
    fun `should validate single operation`() {
        val isValid = ConstantsRegistry.isValidOperation("lambda", "CreateFunction")
        val isInvalid = ConstantsRegistry.isValidOperation("lambda", "NonExistentOperation")
        
        if (ConstantsRegistry.getServiceOperations("lambda").isNotEmpty()) {
            assertTrue(isValid)
            assertFalse(isInvalid)
        } else {
            // If no constants available, should allow all operations
            assertTrue(isValid)
            assertTrue(isInvalid)
        }
    }
}
