/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

open class SchemaGeneratorPluginExtension {
    // TODO Add configuration here (such as codegen configuration)
}

const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "schemaGeneratorPluginExtension"

public class SchemaGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        createExtension()
        configureDependencies()
    }

    private fun Project.createExtension(): SchemaGeneratorPluginExtension = extensions.create<SchemaGeneratorPluginExtension>(SCHEMA_GENERATOR_PLUGIN_EXTENSION)

    private fun Project.configureDependencies() {
        logger.info("Configuring dependencies for schema generation...")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
            // TODO pass plugin configuration to KSP as args...
        }

        val sdkVersion = getSdkVersion()
        dependencies.add("ksp", "aws.sdk.kotlin:dynamodb-mapper-codegen:$sdkVersion")
    }

    // Reads sdk-version.txt for the SDK version to add dependencies on. The file is created in this module's build.gradle.kts
    private fun getSdkVersion(): String = try {
        this.javaClass.getResource("sdk-version.txt")?.readText() ?: throw IllegalStateException("sdk-version.txt does not exist")
    } catch (ex: Exception) {
        throw IllegalStateException("Failed to load sdk-version.txt which sets the SDK version", ex)
    }
}
