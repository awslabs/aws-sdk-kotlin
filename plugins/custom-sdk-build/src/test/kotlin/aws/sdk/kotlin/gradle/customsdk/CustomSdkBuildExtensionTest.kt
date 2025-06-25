/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomSdkBuildExtensionTest {
    
    @Test
    fun `extension can be created and configured`() {
        val project = ProjectBuilder.builder().build()
        val extension = CustomSdkBuildExtension(project)
        
        extension.s3 {
            operations(S3Operation.GetObject, S3Operation.PutObject)
        }
        
        val selectedOperations = extension.getSelectedOperations()
        assertEquals(1, selectedOperations.size)
        assertTrue(selectedOperations.containsKey("s3"))
        assertEquals(2, selectedOperations["s3"]?.size)
    }
    
    @Test
    fun `extension supports multiple services`() {
        val project = ProjectBuilder.builder().build()
        val extension = CustomSdkBuildExtension(project)
        
        extension.s3 {
            operations(S3Operation.GetObject)
        }
        
        extension.dynamodb {
            operations(DynamodbOperation.GetItem, DynamodbOperation.PutItem)
        }
        
        val selectedOperations = extension.getSelectedOperations()
        assertEquals(2, selectedOperations.size)
        assertEquals(1, selectedOperations["s3"]?.size)
        assertEquals(2, selectedOperations["dynamodb"]?.size)
    }
    
    @Test
    fun `validation fails when no services configured`() {
        val project = ProjectBuilder.builder().build()
        val extension = CustomSdkBuildExtension(project)
        
        assertFailsWith<IllegalStateException> {
            extension.validate()
        }
    }
    
    @Test
    fun `validation fails when service has no operations`() {
        val project = ProjectBuilder.builder().build()
        val extension = CustomSdkBuildExtension(project)
        
        extension.s3 {
            // No operations configured
        }
        
        assertFailsWith<IllegalStateException> {
            extension.validate()
        }
    }
    
    @Test
    fun `validation passes with valid configuration`() {
        val project = ProjectBuilder.builder().build()
        val extension = CustomSdkBuildExtension(project)
        
        extension.s3 {
            operations(S3Operation.GetObject)
        }
        
        // Should not throw
        extension.validate()
    }
    
    @Test
    fun `dependency notation can be created`() {
        val project = ProjectBuilder.builder().build()
        val extension = CustomSdkBuildExtension(project)
        
        val dependencyNotation = extension.createDependencyNotation()
        assertNotNull(dependencyNotation)
    }
}
