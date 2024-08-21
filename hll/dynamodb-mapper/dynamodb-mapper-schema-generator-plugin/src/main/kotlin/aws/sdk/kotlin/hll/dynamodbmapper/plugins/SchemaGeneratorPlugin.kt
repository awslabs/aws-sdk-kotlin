/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import com.google.devtools.ksp.gradle.KspExtension

open class SchemaGeneratorPluginExtension

const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "schemaGeneratorPluginExtension"

public class SchemaGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        createExtension()
        configureDependencies()
    }

    private fun Project.createExtension(): SchemaGeneratorPluginExtension = extensions.create<SchemaGeneratorPluginExtension>(SCHEMA_GENERATOR_PLUGIN_EXTENSION)

    private fun Project.configureDependencies() {
        logger.info("Configuring KSP...")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
        }
        dependencies.add("ksp", "aws.sdk.kotlin:dynamodb-mapper-codegen:1.3.2-SNAPSHOT")
    }
}
