/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka") version "0.10.0"
    jacoco
}

val platforms = listOf("common", "jvm")

// Allow subprojects to use internal API's
// See: https://kotlinlang.org/docs/reference/opt-in-requirements.html#opting-in-to-using-api
val experimentalAnnotations = listOf("kotlin.RequiresOptIn", "software.aws.clientrt.util.InternalAPI")

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

subprojects {
    group = "software.aws.kotlin"
    version = "0.0.1"

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin("org.jetbrains.dokka")
    }

    println("Configuring: $project")

    // this works by iterating over each platform name and inspecting the projects files. If the project contains
    // a directory with the corresponding platform name we apply the common configuration settings for that platform
    // (which includes adding the multiplatform target(s)). This makes adding platform support easy and implicit in each
    // subproject.
    platforms.forEach { platform ->
        if (projectNeedsPlatform(project, platform)) {
            configure(listOf(project)){
                println("${project.name} needs platform: $platform")
                apply(from = rootProject.file("gradle/${platform}.gradle"))
            }
        }
    }

    kotlin {
        sourceSets {
            all {
                val srcDir = if (name.endsWith("Main")) "src" else "test"
                val resourcesPrefix = if (name.endsWith("Test")) "test-" else  ""
                // the name is always the platform followed by a suffix of either "Main" or "Test" (e.g. jvmMain, commonTest, etc)
                val platform = name.substring(0, name.length - 4)
                kotlin.srcDir("$platform/$srcDir")
                resources.srcDir("$platform/${resourcesPrefix}resources")
                languageSettings.progressiveMode = true
                experimentalAnnotations.forEach { languageSettings.useExperimentalAnnotation(it) }
            }
        }
    }

    tasks.dokka {
        outputFormat = "html"
        outputDirectory = "$buildDir/kdoc"
    }

    apply(from = rootProject.file("gradle/publish.gradle"))
}

