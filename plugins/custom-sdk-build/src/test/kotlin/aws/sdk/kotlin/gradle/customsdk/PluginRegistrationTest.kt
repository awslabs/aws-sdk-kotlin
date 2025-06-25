/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for plugin registration and SPI configuration.
 */
class PluginRegistrationTest {
    
    @Test
    fun `plugin can be applied by ID`() {
        val project = ProjectBuilder.builder().build()
        
        // Apply plugin by ID
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify plugin was applied
        assertTrue(project.plugins.hasPlugin("aws.sdk.kotlin.custom-sdk-build"))
        assertTrue(project.plugins.hasPlugin(CustomSdkBuildPlugin::class.java))
    }
    
    @Test
    fun `plugin can be applied by class`() {
        val project = ProjectBuilder.builder().build()
        
        // Apply plugin by class
        project.plugins.apply(CustomSdkBuildPlugin::class.java)
        
        // Verify plugin was applied
        assertTrue(project.plugins.hasPlugin(CustomSdkBuildPlugin::class.java))
    }
    
    @Test
    fun `plugin extension is registered correctly`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        // Verify extension is registered
        val extension = project.extensions.findByName("awsCustomSdkBuild")
        assertTrue(extension != null)
        assertTrue(extension is CustomSdkBuildExtension)
    }
    
    @Test
    fun `SPI configuration file exists`() {
        // Verify SPI configuration file exists
        val spiFile = this::class.java.classLoader.getResource(
            "META-INF/services/software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration"
        )
        
        assertTrue(spiFile != null, "SPI configuration file should exist")
        
        // Verify it contains our integration class
        val content = spiFile.readText()
        assertTrue(
            content.contains("aws.sdk.kotlin.gradle.customsdk.CustomSdkDslGeneratorIntegration"),
            "SPI file should contain CustomSdkDslGeneratorIntegration"
        )
    }
    
    @Test
    fun `plugin properties file exists`() {
        // Verify plugin properties file exists
        val propertiesFile = this::class.java.classLoader.getResource(
            "META-INF/gradle-plugins/aws.sdk.kotlin.custom-sdk-build.properties"
        )
        
        assertTrue(propertiesFile != null, "Plugin properties file should exist")
        
        // Verify it contains correct implementation class
        val content = propertiesFile.readText()
        assertTrue(
            content.contains("implementation-class=aws.sdk.kotlin.gradle.customsdk.CustomSdkBuildPlugin"),
            "Properties file should contain correct implementation class"
        )
    }
    
    @Test
    fun `plugin version properties file exists`() {
        // Verify plugin version properties file exists
        val versionFile = this::class.java.classLoader.getResource(
            "META-INF/plugin-version.properties"
        )
        
        assertTrue(versionFile != null, "Plugin version properties file should exist")
        
        // Verify it contains version information
        val content = versionFile.readText()
        assertTrue(content.contains("version="), "Version file should contain version property")
        assertTrue(content.contains("name="), "Version file should contain name property")
        assertTrue(content.contains("description="), "Version file should contain description property")
    }
    
    @Test
    fun `version compatibility checker works`() {
        val project = ProjectBuilder.builder().build()
        
        // Version compatibility check should not throw exceptions
        VersionCompatibility.checkCompatibility(project, project.logger)
        
        // Should be able to get recommendations
        val recommendations = VersionCompatibility.getCompatibilityRecommendations(project)
        assertTrue(recommendations is List<String>)
    }
    
    @Test
    fun `plugin publication metadata is correct`() {
        // Verify plugin publication constants
        assertTrue(PluginPublication.PLUGIN_ID == "aws.sdk.kotlin.custom-sdk-build")
        assertTrue(PluginPublication.PLUGIN_NAME.isNotEmpty())
        assertTrue(PluginPublication.PLUGIN_DESCRIPTION.isNotEmpty())
        assertTrue(PluginPublication.PLUGIN_URL.isNotEmpty())
        
        assertTrue(PluginPublication.GROUP_ID == "aws.sdk.kotlin")
        assertTrue(PluginPublication.ARTIFACT_ID.isNotEmpty())
        
        assertTrue(PluginPublication.LICENSE_NAME.isNotEmpty())
        assertTrue(PluginPublication.LICENSE_URL.isNotEmpty())
        
        assertTrue(PluginPublication.PLUGIN_TAGS.isNotEmpty())
        assertTrue(PluginPublication.PLUGIN_DISPLAY_NAME.isNotEmpty())
        
        assertTrue(PluginPublication.MIN_GRADLE_VERSION.isNotEmpty())
        assertTrue(PluginPublication.MIN_JAVA_VERSION.isNotEmpty())
        assertTrue(PluginPublication.COMPATIBLE_KOTLIN_VERSIONS.isNotEmpty())
    }
    
    @Test
    fun `CustomSdkDslGeneratorIntegration can be instantiated`() {
        // Verify the integration class can be instantiated
        val integration = CustomSdkDslGeneratorIntegration()
        assertTrue(integration != null)
        assertTrue(integration.order == 0.toByte())
    }
    
    @Test
    fun `plugin applies without errors in different project configurations`() {
        // Test with basic project
        val basicProject = ProjectBuilder.builder().build()
        basicProject.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        assertTrue(basicProject.plugins.hasPlugin("aws.sdk.kotlin.custom-sdk-build"))
        
        // Test with project that has repositories
        val projectWithRepos = ProjectBuilder.builder().build()
        projectWithRepos.repositories.mavenCentral()
        projectWithRepos.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        assertTrue(projectWithRepos.plugins.hasPlugin("aws.sdk.kotlin.custom-sdk-build"))
        
        // Test with project that has dependencies
        val projectWithDeps = ProjectBuilder.builder().build()
        projectWithDeps.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        assertTrue(projectWithDeps.plugins.hasPlugin("aws.sdk.kotlin.custom-sdk-build"))
    }
    
    @Test
    fun `plugin works with afterEvaluate configuration`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("aws.sdk.kotlin.custom-sdk-build")
        
        val extension = project.extensions.getByType(CustomSdkBuildExtension::class.java)
        
        // Configure extension
        extension.s3 {
            operations(S3Operation.GetObject)
        }
        
        // Verify configuration works
        val selectedOperations = extension.getSelectedOperations()
        assertTrue(selectedOperations.containsKey("s3"))
        assertTrue(selectedOperations["s3"]?.isNotEmpty() == true)
    }
}
