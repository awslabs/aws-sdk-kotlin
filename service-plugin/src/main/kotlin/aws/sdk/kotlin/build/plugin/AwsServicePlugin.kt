/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 *
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File

class AwsServicePlugin : Plugin<Project> {
    private val AWS_SERVICE_EXTENSION_NAME = "awsService"
    override fun apply(target: Project) = target.run {
        validateModel()
        configurePlugins()
        configureDependencies()
        registerCodegenTasks()

        val extension = installExtension()
    }

    private fun Project.installExtension(): AwsServiceExtension {
        return extensions.create(AWS_SERVICE_EXTENSION_NAME, AwsServiceExtension::class.java, project)
    }

    private fun Project.validateModel() {
        logger.aws("looking for model file in: $awsModelFile")
        if (!awsModelFile.exists()) {
            throw AwsServicePluginException("model not found for project: $name")
        }
    }

    private fun Project.configurePlugins() {
        // TODO - work with smithy team to make configuring this not require the Java plugin
        plugins.apply("software.amazon.smithy")
        tasks.getByName("smithyBuildJar").enabled = false
    }

    private fun Project.configureDependencies() {
        // depend on aws-kotlin code generation
        dependencies.add("compileClasspath", project(":codegen:smithy-aws-kotlin-codegen"))

        // smithy plugin requires smithy-cli to be on the classpath, for whatever reason configuring the plugin
        // from this plugin doesn't work correctly so we explicitly set it
        val smithyVersion: String by project
        dependencies.add("compileClasspath", "software.amazon.smithy:smithy-cli:$smithyVersion")

        // add aws traits to the compile classpath so that the smithy build task can discover them
        dependencies.add("compileClasspath", "software.amazon.smithy:smithy-aws-traits:$smithyVersion")

        // TODO - resolve dependencies from the model file
    }
}

class AwsServicePluginException(message: String) : GradleException(message)

internal val Project.awsModelFile: File
    get() = file("model/service.json")




//plugins {
//    kotlin("jvm")
//}
//
//dependencies {
//    implementation(kotlin("stdlib"))
//    implementation("software.aws.smithy.kotlin:http:0.0.1")
//    implementation("software.aws.smithy.kotlin:http-client-engine-ktor:0.0.1")
//    implementation("aws.sdk.kotlin.runtime:rest-json:0.0.1")
//    implementation("software.aws.smithy.kotlin:serde:0.0.1")
//    implementation("software.aws.smithy.kotlin:serde-json:0.0.1")
//    implementation("software.aws.smithy.kotlin:utils:0.0.1")
//    api("aws.sdk.kotlin.runtime:auth:0.0.1")
//    api("aws.sdk.kotlin.runtime:aws-client-rt:0.0.1")
//    api("software.aws.smithy.kotlin:client-rt-core:0.0.1")
//    api("aws.sdk.kotlin.runtime:http:0.0.1")
//    api("aws.sdk.kotlin.runtime:regions:0.0.1")
//}
//
//val experimentalAnnotations = listOf(
//    "software.aws.clientrt.util.InternalAPI",
//    "aws.sdk.kotlin.runtime.InternalSdkApi"
//)
//kotlin.sourceSets.all {
//    experimentalAnnotations.forEach { languageSettings.useExperimentalAnnotation(it) }
//}
//
//tasks.test {
//    useJUnitPlatform()
//    testLogging {
//        events("passed", "skipped", "failed")
//        showStandardStreams = true
//    }
//}
