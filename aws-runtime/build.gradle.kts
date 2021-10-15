/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

description = "AWS client runtime support for generated service clients"

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
    jacoco
}

val platforms = listOf("common", "jvm")

// Allow subprojects to use internal API's
// See: https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
val optinAnnotations = listOf(
    "kotlin.RequiresOptIn",
)

fun projectNeedsPlatform(project: Project, platform: String ): Boolean {
    val files = project.projectDir.listFiles()
    val hasPosix = files.any { it.name == "posix" }
    val hasDarwin = files.any { it.name == "darwin" }

    if (hasPosix && platform == "darwin") return false
    if (hasDarwin && platform == "posix") return false
    if (!hasPosix && !hasDarwin && platform == "darwin") return false
    // add implicit JVM target if it has a common module
    return files.any{ it.name == platform || (it.name == "common" && platform == "jvm")}
}

kotlin {
    jvm() // Create a JVM target with the default name 'jvm'
}

val sdkVersion: String by project

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin("org.jetbrains.dokka")
    }

    logger.info("configuring: $project")

    // this works by iterating over each platform name and inspecting the projects files. If the project contains
    // a directory with the corresponding platform name we apply the common configuration settings for that platform
    // (which includes adding the multiplatform target(s)). This makes adding platform support easy and implicit in each
    // subproject.
    platforms.forEach { platform ->
        if (projectNeedsPlatform(project, platform)) {
            configure(listOf(project)){
                logger.info("${project.name} needs platform: $platform")
                apply(from = rootProject.file("gradle/${platform}.gradle"))
            }
        }
    }

    kotlin {
        // TODO - refactor this to error `explicitApi()`
        explicitApiWarning()

        sourceSets {
            all {
                val srcDir = if (name.endsWith("Main")) "src" else "test"
                val resourcesPrefix = if (name.endsWith("Test")) "test-" else  ""
                // the name is always the platform followed by a suffix of either "Main" or "Test" (e.g. jvmMain, commonTest, etc)
                val platform = name.substring(0, name.length - 4)
                kotlin.srcDir("$platform/$srcDir")
                resources.srcDir("$platform/${resourcesPrefix}resources")
                languageSettings.progressiveMode = true
                optinAnnotations.forEach { languageSettings.optIn(it) }
            }
        }
    }

    apply(from = rootProject.file("gradle/publish.gradle"))

    dependencies {
        dokkaPlugin(project(":dokka-aws"))
    }
}

// FIXME - resolves build deadlock with aws-core when using composite builds
val topLevelModule = "crt-util"
subprojects.filter { it.name != topLevelModule }.forEach { proj ->
    proj.tasks.findByName("generatePomFileForJvmPublication")?.dependsOn(":aws-runtime:$topLevelModule:generatePomFileForJvmPublication")
}

task<org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport>("rootAllTest"){
    destinationDir = File(project.buildDir, "reports/tests/rootAllTest")
    val rootAllTest = this
    subprojects {
        val proj = this
        afterEvaluate {
            if (tasks.findByName("allTests") != null) {
                val provider = tasks.named("allTests")
                val allTestsTaskProvider = provider as TaskProvider<org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport>
                rootAllTest.addChild(allTestsTaskProvider)
                rootAllTest.dependsOn(allTestsTaskProvider)
            }
        }
    }

    beforeEvaluate {
        project.gradle.taskGraph.whenReady {
            rootAllTest.maybeOverrideReporting(this)
        }
    }
}
