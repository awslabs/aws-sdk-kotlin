/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

// This build file has been adapted from the Go v2 SDK, here:
// https://github.com/aws/aws-sdk-go-v2/blob/master/codegen/sdk-codegen/build.gradle.kts

import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.gradle.tasks.SmithyBuild
import kotlin.streams.toList

plugins {
    id("software.amazon.smithy") version "0.5.1"
}

dependencies {
    implementation(project(":smithy-aws-kotlin-codegen"))
}

// This project doesn't produce a JAR.
tasks["jar"].enabled = false

// Run the SmithyBuild task manually since this project needs the built JAR
tasks["smithyBuildJar"].enabled = false

tasks.create<SmithyBuild>("buildSdk") {
    addRuntimeClasspath = true
}

// force rebuild every time while developing
tasks["build"].outputs.upToDateWhen { false }

// Generates a smithy-build.json file by creating a new projection for every
// JSON file found in aws-models/. The generated smithy-build.json file is
// not committed to git since it's rebuilt each time codegen is performed.
tasks.register("generate-smithy-build") {
    doLast {
        val projectionsBuilder = Node.objectNodeBuilder()
        val modelsDirProp: String by project
        val models = project.file(modelsDirProp)

        fileTree(models).filter { it.isFile }.files.forEach { file ->
            val model = Model.assembler()
                .addImport(file.absolutePath)
                // Grab the result directly rather than worrying about checking for errors via unwrap.
                // All we care about here is the service shape, any unchecked errors will be exposed
                // as part of the actual build task done by the smithy gradle plugin.
                .assemble().result.get()
            val services = model.shapes(ServiceShape::class.javaObjectType).sorted().toList()
            if (services.size != 1) {
                throw Exception("There must be exactly one service in each aws model file, but found " +
                        "${services.size} in ${file.name}: ${services.map { it.id }}")
            }
            val service = services[0]
            var (sdkId, version, _) = file.name.split(".")
            sdkId = sdkId.replace("-", "").toLowerCase()
            val projectionContents = Node.objectNodeBuilder()
                .withMember("imports", Node.fromStrings("${models.absolutePath}${File.separator}${file.name}"))
                .withMember("plugins", Node.objectNode()
                    .withMember("kotlin-codegen", Node.objectNodeBuilder()
                        .withMember("service", service.id.toString())
                        .withMember("module", "aws.sdk.kotlin." + sdkId.toLowerCase())
                        .withMember("moduleVersion", "1.0")
                        .build()))
                .build()
            projectionsBuilder.withMember(sdkId + "." + version.toLowerCase(), projectionContents)
        }

        file("smithy-build.json").writeText(Node.prettyPrintJson(Node.objectNodeBuilder()
            .withMember("version", "1.0")
            .withMember("projections", projectionsBuilder.build())
            .build()))
    }
}

// Run the `buildSdk` automatically.
tasks["build"]
    .dependsOn(tasks["generate-smithy-build"])
    .finalizedBy(tasks["buildSdk"])
