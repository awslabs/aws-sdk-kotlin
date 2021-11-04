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

// The following section exposes Smithy protocol test suites as gradle test targets
// for the configured protocols in [enabledProtocols].
val enabledProtocols = listOf(
    ProtocolTest("aws-ec2-query", "aws.protocoltests.ec2#AwsEc2"),
    ProtocolTest("aws-json-10", "aws.protocoltests.json10#JsonRpc10"),
    ProtocolTest("aws-json-11", "aws.protocoltests.json#JsonProtocol"),
    ProtocolTest("aws-restjson", "aws.protocoltests.restjson#RestJson"),
    ProtocolTest("aws-restxml", "aws.protocoltests.restxml#RestXml"),
    ProtocolTest("aws-restxml-xmlns", "aws.protocoltests.restxml.xmlns#RestXmlWithNamespace"),
    ProtocolTest("aws-query", "aws.protocoltests.query#AwsQuery"),

    // service specific tests
    ProtocolTest("apigateway", "com.amazonaws.apigateway#BackplaneControlService"),
    ProtocolTest("glacier", "com.amazonaws.glacier#Glacier"),
    ProtocolTest("machinelearning", "com.amazonaws.machinelearning#AmazonML_20141212", sdkId = "Machine Learning"),
)

// This project doesn't produce a JAR.
tasks["jar"].enabled = false

// Run the SmithyBuild task manually since this project needs the built JAR
// from smithy-aws-kotlin-codegen.
tasks["smithyBuildJar"].enabled = false

task("generateSmithyBuild") {
    group = "codegen"
    description = "generate smithy-build.json"
    val buildFile = projectDir.resolve("smithy-build.json")
    doFirst {
        buildFile.writeText(generateSmithyBuild(enabledProtocols))
    }
    outputs.file(buildFile)
}

// Remove generated model file for clean
tasks["clean"].doFirst {
    delete("smithy-build.json")
}

tasks.create<SmithyBuild>("generateSdk") {
    group = "codegen"
    // ensure the generated clients use the same version of the runtime as the aws aws-runtime
    val smithyKotlinVersion: String by project
    doFirst {
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinVersion)
    }
    addRuntimeClasspath = true
    dependsOn(tasks["generateSmithyBuild"])
    inputs.file(projectDir.resolve("smithy-build.json"))
    // ensure smithy-aws-kotlin-codegen is up to date
    inputs.files(configurations.compileClasspath)
}

// force rebuild every time while developing
tasks["generateSdk"].outputs.upToDateWhen { false }

data class ProtocolTest(val projectionName: String, val serviceShapeId: String, val sdkId: String? = null) {
    val packageName: String
        get() = projectionName.toLowerCase().filter { it.isLetterOrDigit() }
}


// Generates a smithy-build.json file by creating a new projection.
// The generated smithy-build.json file is not committed to git since
// it's rebuilt each time codegen is performed.
fun generateSmithyBuild(tests: List<ProtocolTest>): String {
    val projections = tests.joinToString(",") { test ->
        val sdkIdEntry = test.sdkId?.let { """"sdkId": "$it",""" } ?: ""
        """
        "${test.projectionName}": {
          "transforms": [
            {
              "name": "includeServices",
              "args": {
                "services": [
                  "${test.serviceShapeId}"
                ]
              }
            }
          ],
          "plugins": {
            "kotlin-codegen": {
              "service": "${test.serviceShapeId}",
              "package": {
                "name": "aws.sdk.kotlin.services.${test.packageName}",
                "version": "1.0"
              },
              $sdkIdEntry
              "build": {
                "rootProject": true,
                "optInAnnotations": [
                  "aws.smithy.kotlin.runtime.util.InternalApi",
                  "aws.sdk.kotlin.runtime.InternalSdkApi"
                ]
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

    /**
     * The build directory for the task
     */
    val generatedBuildDir: File
        @OutputDirectory
        get() = project.buildDir.resolve("smithyprojections/${project.name}/$protocol/$plugin")

    @TaskAction
    fun runTests() {
        require(protocol.isNotEmpty()) { "protocol name must be specified" }
        require(plugin.isNotEmpty()) { "plugin name must be specified" }

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

enabledProtocols.forEach {
    val protocolName = it.projectionName

    val protocolTestTask = tasks.register<ProtocolTestTask>("testProtocol-$protocolName") {
        dependsOn(tasks["generateSdk"])
        group = "Verification"
        protocol = protocolName
        plugin = "kotlin-codegen"
    }.get()

    // FIXME This is a hack to work around how protocol tests aren't in the actual service model and thus codegen
    // separately from service customizations.
    tasks.create<Copy>("copyStaticFiles-$protocolName") {
        from(rootProject.projectDir.resolve("services/$protocolName/common/src"))
        into(protocolTestTask.generatedBuildDir.resolve("src/main/kotlin/"))
        tasks["generateSdk"].finalizedBy(this)
    }
}

tasks.register("testAllProtocols") {
    group = "Verification"
    val allTests = tasks.withType<ProtocolTestTask>()
    dependsOn(allTests)
}
