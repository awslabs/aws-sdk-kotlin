/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

pluginManagement {
    repositories {
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }

        gradlePluginPortal()
    }
}

rootProject.name = "aws-sdk-kotlin"
enableFeaturePreview("GRADLE_METADATA")


fun module(path: String) {
    val name = path.replace('\\', '/').substringAfterLast('/')
    include(name)
    project(":$name").projectDir = file(path)
}


module("codegen")
module("codegen/sdk-codegen")
module("codegen/smithy-aws-kotlin-codegen")
module("codegen/protocol-test-codegen")
include(":client-runtime")
include(":client-runtime:aws-client-rt")
include(":client-runtime:testing")
include(":client-runtime:regions")
include(":client-runtime:auth")
include(":client-runtime:protocols:http")
include(":client-runtime:protocols:rest-json")
include(":client-runtime:crt-util")

// service client examples/playground for exploring design space
module("design/lambda-example")
module("design/s3-example")


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
