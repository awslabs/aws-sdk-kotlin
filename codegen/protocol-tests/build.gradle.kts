/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import software.amazon.smithy.gradle.tasks.SmithyBuild

plugins {
    id("software.amazon.smithy")
}

description = "Smithy protocol test suite"

buildscript {
    val smithyVersion: String by project
    dependencies {
        classpath("software.amazon.smithy:smithy-cli:$smithyVersion")
    }
}


val smithyVersion: String by project
dependencies {
    implementation("software.amazon.smithy:smithy-aws-protocol-tests:$smithyVersion")
    implementation(project(":codegen:smithy-aws-kotlin-codegen"))
}

// This project doesn't produce a JAR.
tasks["jar"].enabled = false

// Run the SmithyBuild task manually since this project needs the built JAR
// from smithy-aws-kotlin-codegen.
tasks["smithyBuildJar"].enabled = false

tasks.create<SmithyBuild>("buildSdk") {
    // ensure the generated clients use the same version of the runtime as the aws client-runtime
    val smithyKotlinClientRtVersion: String by project
    doFirst {
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinClientRtVersion)
    }
    addRuntimeClasspath = true
}

// Run the `buildSdk` automatically.
tasks["build"].finalizedBy(tasks["buildSdk"])

// force rebuild every time while developing
tasks["buildSdk"].outputs.upToDateWhen { false }


open class ProtocolTestTask : DefaultTask() {
    /**
     * The protocol name
     */
    @get:Input
    var protocol: String = ""

    /**
     * The plugin name to use
     */
    @get:Input
    var plugin: String = ""

    @TaskAction
    fun runTests(){
        require(protocol.isNotEmpty()) { "protocol name must be specified" }
        require(plugin.isNotEmpty()) { "plugin name must be specified" }

        val generatedBuildDir = project.file("${project.buildDir}/smithyprojections/${project.name}/$protocol/$plugin")
        println("[$protocol] buildDir: $generatedBuildDir")
        if (!generatedBuildDir.exists()) {
            throw GradleException("$generatedBuildDir does not exist")
        }
        val gradlew = project.rootProject.file("gradlew").absolutePath

        // FIXME - this still requires us to publish to maven local.
        // We might be able to do something clever with an init script by overriding dependencies or something
        // and passing as a cli arg to gradle invocation
        // https://docs.gradle.org/current/userguide/init_scripts.html
        project.exec {
            workingDir = generatedBuildDir
            executable = gradlew
            args = listOf("test")
        }
    }
}

// The following section exposes Smithy protocol test suites as gradle test targets
// for the configured protocols in [enabledProtocols].
val enabledProtocols = listOf("aws-json-10", "aws-json-11", "aws-restjson")

enabledProtocols.forEach {
    tasks.register<ProtocolTestTask>("testProtocol-${it}") {
        dependsOn(tasks.build)
        group = "Verification"
        protocol = it
        plugin = "kotlin-codegen"
    }
}


tasks.register("testAllProtocols") {
    group = "Verification"
    val allTests = tasks.withType<ProtocolTestTask>()
    dependsOn(allTests)
}
