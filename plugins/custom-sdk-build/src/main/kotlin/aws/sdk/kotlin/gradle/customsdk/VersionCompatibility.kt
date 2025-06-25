/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.logging.Logger

/**
 * Version compatibility checker for the custom SDK build plugin.
 * Ensures that the plugin version matches the AWS SDK runtime version.
 */
object VersionCompatibility {
    
    /**
     * Check version compatibility between plugin and SDK runtime.
     */
    fun checkCompatibility(project: Project, logger: Logger) {
        try {
            val pluginVersion = getPluginVersion()
            val expectedSdkVersion = pluginVersion // Plugin version should match SDK version
            
            logger.info("Custom SDK Build Plugin version: $pluginVersion")
            logger.info("Expected AWS SDK version: $expectedSdkVersion")
            
            // Check if AWS SDK dependencies are present and compatible
            checkAwsSdkDependencies(project, expectedSdkVersion, logger)
            
            // Validate Gradle version compatibility
            checkGradleCompatibility(project, logger)
            
            // Validate Kotlin version compatibility
            checkKotlinCompatibility(project, logger)
            
        } catch (e: Exception) {
            logger.warn("Version compatibility check failed: ${e.message}")
            // Don't fail the build for version check issues
        }
    }
    
    /**
     * Get the plugin version from resources.
     */
    private fun getPluginVersion(): String {
        return try {
            val properties = java.util.Properties()
            val inputStream = VersionCompatibility::class.java.classLoader
                .getResourceAsStream("META-INF/plugin-version.properties")
            
            if (inputStream != null) {
                properties.load(inputStream)
                properties.getProperty("version", "unknown")
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * Check AWS SDK dependency compatibility.
     */
    private fun checkAwsSdkDependencies(project: Project, expectedVersion: String, logger: Logger) {
        // Check if any AWS SDK dependencies are present
        val configurations = project.configurations
        var foundAwsSdk = false
        
        configurations.forEach { config ->
            config.dependencies.forEach { dependency ->
                if (dependency.group == "aws.sdk.kotlin") {
                    foundAwsSdk = true
                    val dependencyVersion = dependency.version
                    
                    if (dependencyVersion != null && dependencyVersion != expectedVersion) {
                        logger.warn(
                            "AWS SDK version mismatch: " +
                            "Plugin expects $expectedVersion but found $dependencyVersion for ${dependency.name}. " +
                            "Consider using matching versions for optimal compatibility."
                        )
                    }
                }
            }
        }
        
        if (!foundAwsSdk) {
            logger.info("No AWS SDK dependencies detected. Custom SDK will be the primary AWS SDK dependency.")
        }
    }
    
    /**
     * Check Gradle version compatibility.
     */
    private fun checkGradleCompatibility(project: Project, logger: Logger) {
        val gradleVersion = project.gradle.gradleVersion
        val minGradleVersion = "7.0"
        
        if (compareVersions(gradleVersion, minGradleVersion) < 0) {
            logger.warn(
                "Gradle version $gradleVersion may not be fully compatible. " +
                "Minimum recommended version is $minGradleVersion."
            )
        } else {
            logger.debug("Gradle version $gradleVersion is compatible")
        }
    }
    
    /**
     * Check Kotlin version compatibility.
     */
    private fun checkKotlinCompatibility(project: Project, logger: Logger) {
        try {
            // Check if Kotlin plugin is applied
            val hasKotlinJvm = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")
            val hasKotlinMultiplatform = project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
            
            if (hasKotlinJvm || hasKotlinMultiplatform) {
                logger.debug("Kotlin plugin detected - compatibility check passed")
            } else {
                logger.info(
                    "No Kotlin plugin detected. " +
                    "Apply kotlin(\"jvm\") or kotlin(\"multiplatform\") plugin for optimal integration."
                )
            }
        } catch (e: Exception) {
            logger.debug("Kotlin compatibility check failed: ${e.message}")
        }
    }
    
    /**
     * Compare two version strings.
     * Returns negative if v1 < v2, zero if v1 == v2, positive if v1 > v2.
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(parts1.size, parts2.size)
        
        for (i in 0 until maxLength) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0
            
            when {
                part1 < part2 -> return -1
                part1 > part2 -> return 1
            }
        }
        
        return 0
    }
    
    /**
     * Get compatibility recommendations for the user.
     */
    fun getCompatibilityRecommendations(project: Project): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Check Gradle version
        val gradleVersion = project.gradle.gradleVersion
        if (compareVersions(gradleVersion, "7.0") < 0) {
            recommendations.add("Upgrade Gradle to version 7.0 or later for optimal compatibility")
        }
        
        // Check Kotlin plugin
        val hasKotlinPlugin = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") ||
                project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
        
        if (!hasKotlinPlugin) {
            recommendations.add("Apply the Kotlin plugin: kotlin(\"jvm\") or kotlin(\"multiplatform\")")
        }
        
        // Check build cache
        if (!project.gradle.startParameter.isBuildCacheEnabled) {
            recommendations.add("Enable Gradle build cache for better performance: --build-cache or org.gradle.caching=true")
        }
        
        return recommendations
    }
}
