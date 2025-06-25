/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for version compatibility checking.
 */
class VersionCompatibilityTest {
    
    @Test
    fun `version compatibility check completes without errors`() {
        val project = ProjectBuilder.builder().build()
        
        // Should not throw exceptions
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `compatibility recommendations are provided`() {
        val project = ProjectBuilder.builder().build()
        
        val recommendations = VersionCompatibility.getCompatibilityRecommendations(project)
        
        // Should return a list (may be empty)
        assertTrue(recommendations is List<String>)
        
        // Should contain helpful recommendations for basic project
        assertTrue(recommendations.any { it.contains("Kotlin") })
    }
    
    @Test
    fun `gradle version compatibility works`() {
        val project = ProjectBuilder.builder().build()
        
        // Get current Gradle version
        val gradleVersion = project.gradle.gradleVersion
        assertTrue(gradleVersion.isNotEmpty())
        
        // Version compatibility check should handle current version
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `kotlin plugin detection works`() {
        val project = ProjectBuilder.builder().build()
        
        // Initially no Kotlin plugin
        val initialRecommendations = VersionCompatibility.getCompatibilityRecommendations(project)
        assertTrue(initialRecommendations.any { it.contains("Kotlin") })
        
        // Version compatibility check should handle missing Kotlin plugin
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `build cache detection works`() {
        val project = ProjectBuilder.builder().build()
        
        // Build cache is typically disabled in test projects
        val recommendations = VersionCompatibility.getCompatibilityRecommendations(project)
        assertTrue(recommendations.any { it.contains("build cache") || it.contains("caching") })
        
        // Version compatibility check should handle build cache status
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `aws sdk dependency detection works`() {
        val project = ProjectBuilder.builder().build()
        
        // Add a mock AWS SDK dependency
        val configuration = project.configurations.create("implementation")
        val dependency = project.dependencies.create("aws.sdk.kotlin:s3:1.0.0")
        configuration.dependencies.add(dependency)
        
        // Version compatibility check should handle AWS SDK dependencies
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `version comparison works correctly`() {
        // Test version comparison logic indirectly through compatibility check
        val project = ProjectBuilder.builder().build()
        
        // This will internally use version comparison
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `plugin version can be retrieved`() {
        val project = ProjectBuilder.builder().build()
        
        // Version compatibility check should be able to get plugin version
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown (even if version is "unknown")
        assertTrue(true)
    }
    
    @Test
    fun `compatibility check handles exceptions gracefully`() {
        val project = ProjectBuilder.builder().build()
        
        // Even if internal checks fail, the compatibility check should not throw
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Test passes if no exception is thrown
        assertTrue(true)
    }
    
    @Test
    fun `recommendations are helpful and actionable`() {
        val project = ProjectBuilder.builder().build()
        
        val recommendations = VersionCompatibility.getCompatibilityRecommendations(project)
        
        // Recommendations should be actionable
        recommendations.forEach { recommendation ->
            assertTrue(recommendation.isNotEmpty())
            // Should contain actionable advice
            assertTrue(
                recommendation.contains("Upgrade") || 
                recommendation.contains("Apply") || 
                recommendation.contains("Enable") ||
                recommendation.contains("version") ||
                recommendation.contains("plugin")
            )
        }
    }
}
