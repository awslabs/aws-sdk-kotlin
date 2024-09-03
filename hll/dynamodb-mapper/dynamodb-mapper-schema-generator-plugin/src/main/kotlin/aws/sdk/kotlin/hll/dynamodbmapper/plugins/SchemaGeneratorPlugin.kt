/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.AnnotationsProcessorOptions
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

public class SchemaGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        val extension = createExtension()
        configureDependencies()

        project.afterEvaluate {
            val dstPkgSerialized = when (val dstPkg = extension.destinationPackage) {
                is DestinationPackage.Relative -> "relative=${dstPkg.pkg}"
                is DestinationPackage.Absolute -> "absolute=${dstPkg.pkg}"
            }

            extensions.configure<KspExtension> {
                arg(AnnotationsProcessorOptions.GenerateBuilderClassesAttribute.name, extension.generateBuilderClasses.name)
                arg(AnnotationsProcessorOptions.VisibilityAttribute.name, extension.visibility.name)
                arg(AnnotationsProcessorOptions.DestinationPackageAttribute.name, dstPkgSerialized)
                arg(AnnotationsProcessorOptions.GenerateGetTableMethodAttribute.name, extension.generateGetTableExtension.toString())
            }
        }
    }

    private fun Project.createExtension(): SchemaGeneratorPluginExtension = extensions.create<SchemaGeneratorPluginExtension>(SCHEMA_GENERATOR_PLUGIN_EXTENSION)

    private fun Project.configureDependencies() {
        logger.info("Configuring dependencies for schema generation...")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            excludeProcessor("aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.HighLevelOpsProcessorProvider")
        }

        val sdkVersion = getSdkVersion()
        dependencies.add("ksp", "aws.sdk.kotlin:dynamodb-mapper-codegen:$sdkVersion")
    }

    // Reads sdk-version.txt for the SDK version to add dependencies on. The file is created in this module's build.gradle.kts
    private fun getSdkVersion(): String = checkNotNull(this::class.java.getResource("sdk-version.txt")?.readText()) { "Could not read sdk-version.txt" }
}
