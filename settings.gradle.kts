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

val compositeProjectList = try {
    val localProperties = java.util.Properties()
    localProperties.load(File(rootProject.projectDir, "local.properties").inputStream())
    localProperties.getProperty("compositeProjects")
        ?.splitToSequence(",")
        ?.map { file(it) }
        ?.toList()
        ?: emptyList()
} catch (e: Throwable) {
    listOf(file("../smithy-kotlin"))
}

compositeProjectList.filter { it.exists() }.forEach {
    println("Including build '$it'")
    includeBuild(it)
}
