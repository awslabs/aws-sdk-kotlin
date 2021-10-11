/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    // configure default plugin versions
    plugins {
        val kotlinVersion: String by settings
        val dokkaVersion: String by settings
        val smithyGradleVersion: String by settings
        id("org.jetbrains.dokka") version dokkaVersion
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.multiplatform") version kotlinVersion
        id("software.amazon.smithy") version smithyGradleVersion
    }
}

rootProject.name = "aws-sdk-kotlin"

include(":dokka-aws")
include(":codegen:sdk")
include(":codegen:smithy-aws-kotlin-codegen")
include(":codegen:protocol-tests")
include(":aws-runtime")
include(":aws-runtime:testing")
include(":aws-runtime:aws-core")
include(":aws-runtime:aws-types")
include(":aws-runtime:aws-config")
include(":aws-runtime:aws-endpoint")
include(":aws-runtime:aws-signing")
include(":aws-runtime:aws-http")
include(":aws-runtime:http-client-engine-crt")
include(":aws-runtime:protocols:aws-json-protocols")
include(":aws-runtime:protocols:aws-xml-protocols")
include(":aws-runtime:protocols:aws-event-stream")
include(":aws-runtime:crt-util")

// generated services
fun File.isServiceDir(): Boolean {
    if (isDirectory)  {
        return toPath().resolve("build.gradle.kts").toFile().exists()
    }
    return false
}

file("services").listFiles().forEach {
    if (it.isServiceDir()) {
        include(":services:${it.name}")
    }
}

/**
 * The following code enables to optionally include aws-sdk-kotlin dependencies in source form for easier
 * development.  By default, if `smithy-kotlin` exists as a directory at the same level as `aws-sdk-kotlin`
 * then `smithy-kotlin` will be added as a composite build.  To override this behavior, for example to add
 * more composite builds, specify a different directory for `smithy-kotlin`, or to disable the feature entirely,
 * a local.properties file can be added or amended such that the property `compositeProjects` specifies
 * a comma delimited list of paths to project roots that shall be added as composite builds.  If the list is
 * empty to builds will be added.  Invalid directories are ignored.  Example local.properties:
 *
 * compositeProjects=~/repos/smithy-kotlin,/tmp/some/other/thing,../../another/project
 *
 */
val compositeProjectList = try {
    val localProperties = java.util.Properties()
    localProperties.load(File(rootProject.projectDir, "local.properties").inputStream())
    val filePaths = localProperties.getProperty("compositeProjects")
        ?.splitToSequence(",")  // Split comma delimited string into sequence
        ?.map { it.replaceFirst("^~".toRegex(), System.getProperty("user.home")) } // expand user dir
        ?.filter { it.isNotBlank() }
        ?.map { file(it) } // Create file from path
        ?.toList()
        ?: emptyList()

    if (filePaths.isNotEmpty()) println("Adding ${filePaths.size} composite build directories from local.properties.")
    filePaths
} catch (e: java.io.FileNotFoundException) {
    listOf(file("../smithy-kotlin")) // Default path, not an error.
} catch (e: Throwable) {
    logger.error("Failed to load project paths from local.properties. Assuming defaults.", e)
    listOf(file("../smithy-kotlin"))
}

compositeProjectList.forEach { projectRoot ->
    when (projectRoot.exists()) {
        true -> {
            println("Including build '$projectRoot'")
            includeBuild(projectRoot)
        }
        false -> println("Ignoring invalid build directory '$projectRoot'.")
    }
}
