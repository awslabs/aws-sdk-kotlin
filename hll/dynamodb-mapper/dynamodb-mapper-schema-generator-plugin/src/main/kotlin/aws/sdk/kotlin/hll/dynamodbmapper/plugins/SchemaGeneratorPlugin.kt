/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

open class SchemaGeneratorPluginExtension

const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "schemaGeneratorPluginExtension"

public class SchemaGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        createExtension()
        registerTasks()
    }

    private fun Project.createExtension(): SchemaGeneratorPluginExtension = extensions.create<SchemaGeneratorPluginExtension>(SCHEMA_GENERATOR_PLUGIN_EXTENSION)

    private fun Project.registerTasks() {
        task("generateSchemas") {
            println("Generating schemas for classes annotated with DynamoDbItem...")
        }
    }
}
