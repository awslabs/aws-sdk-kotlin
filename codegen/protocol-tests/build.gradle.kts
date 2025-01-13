/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionPath
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir

plugins {
    kotlin("jvm")
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
}

description = "Smithy protocol test suite"

data class ProtocolTest(val projectionName: String, val serviceShapeId: String, val sdkId: String? = null) {
    val packageName: String = projectionName.lowercase().filter { it.isLetterOrDigit() }
}

// The following section exposes Smithy protocol test suites as gradle test targets
// for the configured protocols in [enabledProtocols].
val enabledProtocols = listOf(
    // service specific tests
    ProtocolTest("apigateway", "com.amazonaws.apigateway#BackplaneControlService"),
    ProtocolTest("glacier", "com.amazonaws.glacier#Glacier"),
    ProtocolTest("machinelearning", "com.amazonaws.machinelearning#AmazonML_20141212", sdkId = "Machine Learning"),
)

smithyBuild {
    enabledProtocols.forEach { test ->
        projections.register(test.projectionName) {
            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["${test.serviceShapeId}"]
                  }
                }
                """,
            )

            smithyKotlinPlugin {
                serviceShapeId = test.serviceShapeId
                packageName = "aws.sdk.kotlin.services.${test.packageName}"
                packageVersion = "1.0"
                sdkId = test.sdkId
                buildSettings {
                    generateFullProject = true
                    optInAnnotations = listOf(
                        "aws.smithy.kotlin.runtime.InternalApi",
                        "aws.sdk.kotlin.runtime.InternalSdkApi",
                    )
                }
                apiSettings {
                    defaultValueSerializationMode = "always"
                }
            }
        }
    }
}

val codegen by configurations.getting
dependencies {
    codegen(project(":codegen:aws-sdk-codegen"))
    codegen(libs.smithy.cli)
    codegen(libs.smithy.model)

    // NOTE: The protocol tests are published to maven as a jar, this ensures that
    // the aws-protocol-tests dependency is found when generating code such that the `includeServices` transform
    // actually works
    codegen(libs.smithy.aws.protocol.tests)
}

tasks.generateSmithyProjections {
    // ensure the generated clients use the same version of the runtime as the aws aws-runtime
    // val smithyKotlinRuntimeVersion = libs.versions.smithy.kotlin.runtime.version.get()\

    // align with smithy version to pass protocol test, will change back before merge
    val smithyKotlinRuntimeVersion = "1.3.33-SNAPSHOT"
    doFirst {
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinRuntimeVersion)
    }
}

abstract class ProtocolTestTask @Inject constructor(private val project: Project) : DefaultTask() {
    /**
     * The projection
     */
    @get:Input
    abstract val projectionName: Property<String>

    /**
     * The projection root directory
     */
    @get:Input
    abstract val projectionRootDirectory: Property<String>

    @TaskAction
    fun runTests() {
        val projectionRootDir = project.file(projectionRootDirectory.get())
        println("[$projectionName] buildDir: $projectionRootDir")
        if (!projectionRootDir.exists()) {
            throw GradleException("$projectionRootDir does not exist")
        }
        val wrapper = if (System.getProperty("os.name").lowercase().contains("windows")) "gradlew.bat" else "gradlew"
        val gradlew = project.rootProject.file(wrapper).absolutePath

        // NOTE - this still requires us to publish to maven local.
        project.exec {
            workingDir = projectionRootDir
            executable = gradlew
            args = listOf("test")
        }
    }
}

smithyBuild.projections.forEach {
    val protocolName = it.name

    tasks.register<ProtocolTestTask>("testProtocol-$protocolName") {
        dependsOn(tasks.generateSmithyProjections)
        group = "Verification"
        projectionName.set(it.name)
        projectionRootDirectory.set(smithyBuild.smithyKotlinProjectionPath(it.name).map { it.toString() })
    }

    // FIXME This is a hack to work around how protocol tests aren't in the actual service model and thus codegen
    // separately from service customizations.
    val copyStaticFiles = tasks.register<Copy>("copyStaticFiles-$protocolName") {
        group = "codegen"
        from(rootProject.projectDir.resolve("services/$protocolName/common/src"))
        into(smithyBuild.smithyKotlinProjectionSrcDir(it.name))
    }

    tasks.generateSmithyProjections.configure {
        finalizedBy(copyStaticFiles)
    }
}

tasks.register("testAllProtocols") {
    group = "Verification"
    val allTests = tasks.withType<ProtocolTestTask>()
    dependsOn(allTests)
}
