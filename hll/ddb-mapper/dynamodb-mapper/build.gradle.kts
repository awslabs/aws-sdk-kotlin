/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.SmithyBuildPluginSettings
import aws.sdk.kotlin.gradle.codegen.dsl.SmithyKotlinPluginSettings
import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir
import software.amazon.smithy.model.node.Node

plugins {
    id("ddbmapper-ops-codegen") // The ops-codegen subproject
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":services:dynamodb"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

val codegen by configurations.getting
dependencies {
    codegen(project(":hll:ddb-mapper:ddb-mapper-ops-codegen"))
    codegen(libs.smithy.cli)
    codegen(libs.smithy.model)
}

fun awsModelFile(name: String): String =
    rootProject.file("codegen/sdk/aws-models/$name").relativeTo(project.layout.buildDirectory.get().asFile).toString()

class DdbMapperOpsCodegenPluginSettings : SmithyKotlinPluginSettings() {
    override val pluginName = "ddb-mapper-ops-codegen" // defined in DdbMapperOpsCodegenPlugin.kt
}

smithyBuild {
    projections {
        create("ddb-mapper-ops") {
            imports = listOf(awsModelFile("dynamodb.json"))

            plugins.computeIfAbsent("ddb-mapper-ops-codegen") {
                DdbMapperOpsCodegenPluginSettings().apply {
                    serviceShapeId = "com.amazonaws.dynamodb#DynamoDB_20120810"
                    packageName = "aws.sdk.kotlin.hll.dynamodbmapper.operations"
                    packageVersion = project.version.toString()
                    sdkId = "DynamoDB"
                }
            }
        }
    }
}

/*
NOTE: We need the following tasks to depend on codegen for gradle caching/up-to-date checks to work correctly:

* `compileKotlinJvm` (Type=KotlinCompile)
* `compileKotlinMetadata` (Type=KotlinCompileCommon)
* `sourcesJar` and `jvmSourcesJar` (Type=org.gradle.jvm.tasks.Jar)
*
* For Kotlin/Native, an additional dependency is introduced:
* `compileKotlin<Platform>` (Type=KotlinNativeCompile) (e.g. compileKotlinLinuxX64)
*/
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(tasks.generateSmithyProjections)

    compilerOptions {
        // generated sts/sso credential providers have quite a few warnings
        allWarningsAsErrors.set(false)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    dependsOn(tasks.generateSmithyProjections)
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon> {
    dependsOn(tasks.generateSmithyProjections)
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    if (name == "jar") {
        println("Disabling $project task '$name' because it conflicts with Kotlin JAR tasks")
        enabled = false
    } else {
        dependsOn(tasks.generateSmithyProjections)
    }
}

smithyBuild.projections.all {
    // add this projected source dir to the common sourceSet
    val projectionSrcDir = smithyBuild.smithyKotlinProjectionSrcDir(name)
    kotlin.sourceSets.commonMain {
        logger.info("added $projectionSrcDir to common sourceSet")
        kotlin.srcDir(projectionSrcDir)
    }
}
