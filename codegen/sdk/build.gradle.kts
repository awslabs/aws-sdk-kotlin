/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

// This build file has been adapted from the Go v2 SDK, here:
// https://github.com/aws/aws-sdk-go-v2/blob/master/codegen/sdk-codegen/build.gradle.kts

import software.amazon.smithy.gradle.tasks.SmithyBuild
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import java.util.*
import kotlin.streams.toList

description = "AWS SDK codegen tasks"

plugins {
    id("software.amazon.smithy") version "0.5.2"
}

buildscript {
    val smithyVersion: String by project
    dependencies {
        classpath("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    }
}

dependencies {
    implementation(project(":codegen:smithy-aws-kotlin-codegen"))
}

// This project doesn't produce a JAR.
tasks["jar"].enabled = false

// Run the SmithyBuild task manually since this project needs the built JAR
tasks["smithyBuildJar"].enabled = false

// get a project property by name if it exists (including from local.properties)
fun getProperty(name: String): String? {
    if (project.hasProperty(name)) {
        return project.properties[name].toString()
    }

    val localProperties = Properties()
    val propertiesFile: File = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { localProperties.load(it) }

        if (localProperties.containsKey(name)) {
            return localProperties[name].toString()
        }
    }
    return null
}

// Represents information needed to generate a smithy projection JSON stanza
data class AwsService(
    val name: String,
    val moduleName: String,
    val moduleVersion: String = "1.0",
    val modelFile: File,
    val projectionName: String,
    val sdkId: String,
    val description: String = ""
)

// Generates a smithy-build.json file by creating a new projection.
// The generated smithy-build.json file is not committed to git since
// it's rebuilt each time codegen is performed.
fun generateSmithyBuild(services: List<AwsService>): String {

    val projections = services.joinToString(",") { service ->
        // escape windows paths for valid json
        val absModelPath = service.modelFile.absolutePath.replace("\\", "\\\\")
        """
            "${service.projectionName}": {
                "imports": ["$absModelPath"],
                "plugins": {
                    "kotlin-codegen": {
                      "service": "${service.name}",
                      "module": "${service.moduleName}",
                      "moduleVersion": "${service.moduleVersion}",
                      "moduleDescription": "${service.description}",
                      "sdkId": "${service.sdkId}",
                      "build": {
                          "generateDefaultBuildFiles": false
                      }
                    }
                }
            }
        """
    }
    return """
    {
        "version": "1.0",
        "projections": {
            $projections
        }
    }
    """.trimIndent()
}

val discoveredServices: List<AwsService> by lazy { discoverServices() }

// Returns an AwsService model for every JSON file found in in directory defined by property `modelsDirProp`
fun discoverServices(): List<AwsService> {
    val modelsDir: String by project
    val serviceIncludeList = getProperty("aws.services")?.split(",")?.map { it.trim() }

    return fileTree(project.file(modelsDir))
        .filter {
            val includeModel = serviceIncludeList?.contains(it.name.split(".").first()) ?: true
            if (!includeModel) {
                logger.info("skipping ${it.absolutePath}, name not included in aws.services")
            }
            includeModel
        }
        .map { file ->
            val model = Model.assembler().addImport(file.absolutePath).assemble().result.get()
            val services: List<ServiceShape> = model.shapes(ServiceShape::class.java).sorted().toList()
            require(services.size == 1) { "Expected one service per aws model, but found ${services.size} in ${file.absolutePath}: ${services.map { it.id }}" }
            val service = services.first()
            val serviceApi = service.getTrait(software.amazon.smithy.aws.traits.ServiceTrait::class.java).orNull()
                ?: error { "Expected aws.api#service trait attached to model ${file.absolutePath}" }
            val (name, version, _) = file.name.split(".")

            val description = service.getTrait(software.amazon.smithy.model.traits.TitleTrait::class.java).map { it.value }.orElse("")

            logger.info("discovered service: ${serviceApi.sdkId}")

            AwsService(
                name = service.id.toString(),
                moduleName = "aws.sdk.kotlin.$name",
                modelFile = file,
                projectionName = name + "." + version.toLowerCase(),
                sdkId = serviceApi.sdkId,
                description = description
            )
        }
}

fun <T> java.util.Optional<T>.orNull(): T? = this.orElse(null)

// Generate smithy-build.json as first step in build task
task("generateSmithyBuild") {
    description = "generate smithy-build.json"
    doFirst {
        projectDir.resolve("smithy-build.json").writeText(generateSmithyBuild(discoveredServices))
    }
}

tasks.create<SmithyBuild>("generateSdk") {
    addRuntimeClasspath = true
    dependsOn(tasks["generateSmithyBuild"])
    inputs.file(projectDir.resolve("smithy-build.json"))
}

// Remove generated model file for clean
tasks["clean"].doFirst {
    delete("smithy-build.json")
}

val AwsService.outputDir: String
    get() = project.file("${project.buildDir}/smithyprojections/${project.name}/${projectionName}/kotlin-codegen").absolutePath

val AwsService.destinationDir: String
    get(){
        val sanitizedName = projectionName.split(".")[0]
        return rootProject.file("services/${sanitizedName}").absolutePath
    }

task("stageSdks") {
    description = "relocate generated SDK(s) from build directory to services/ dir"
    dependsOn("generateSdk")
    doLast {
        discoveredServices.forEach {
            logger.info("copying ${it.outputDir} to ${it.destinationDir}")
            copy {
                from(it.outputDir)
                into(it.destinationDir)
            }
        }
    }
}

tasks.create("bootstrap") {
    description = "Generate AWS SDK's and register them with the build"

    dependsOn(tasks["generateSdk"])
    finalizedBy(tasks["stageSdks"])
}
